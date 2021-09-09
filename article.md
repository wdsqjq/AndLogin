# Hook AMS + APT实现集中式登录框架

#### 1， 背景

登录功能是App开发中一个很常见的功能，一般存在两种登录方式：

- 一种是进入应用就必须先登录才能使用（如聊天类软件）

- 另一种是以游客身份使用，需要登录的时候才会去登录（如商城类软件）

针对第二种的登录方式，一般都是在要跳转到需要登录才能访问的页面（以下简称**目标页面**）时通过`if-else`判断是否已登录，未登录则跳转到登录界面，登录成功后退回到原界面，用户继续进行操作。伪代码如下：

```java
if (需要登录) {
	// 跳转到登录页面
} else {
	// 跳转到目标页面
}
```

这中方式存在着以下几方面问题：

1. 当项目功能逐渐庞大以后，存在大量重复的用于判断登录的代码，且判断逻辑可能分布在不同模块，维护成本很高。
2. 增加或删除目标页面时需要修改判断逻辑，存在耦合。
3. 跳转到登录页面，登录成功后只能退回到原界面，用户原本的意图被打断，需要再次点击才能进入目标界面（如：用户在个人中心界面点击“我的订单”按钮想要跳转到订单界面，由于没有登录就跳转到了登录界面，登录成功后返回个人中心界面，用户需要再次点击“我的订单”按钮才能进入订单界面）。

大致流程如下图所示：

![login](/img/login.png)



针对传统登录方案存在的问题本文提出了一种**通过Hook AMS + APT实现集中式登录**方案。

1. 首先通过Hook AMS实现集中处理判断，实现了跟业务逻辑解耦。

2. 通过注解标记需要登录的页面，然后通过APT生成需要登录页面的集合，便于Hook中的判断。

3. 最后在Hook AMS时将原意图放入登录页面的意图中，登录页面登录成功后可以获取到原意图，实现了继续用户原意图的目的。

本方案能达到的业务流程如下：

![login](/img/hook_login.png)



#### 1， 集中处理

这里借鉴插件化的思路通过Hook AMS实现拦截并统一处理的目的

##### 1.1 分析Activity启动过程

了解Activity启动过程的应该都知道Activity中的`startActivity()`最终会进入`Instrumentation`：

```java
// Activity.java
@Override
public void startActivityForResult(
        String who, Intent intent, int requestCode, @Nullable Bundle options) {
    ...
    Instrumentation.ActivityResult ar =
        mInstrumentation.execStartActivity(
            this, mMainThread.getApplicationThread(), mToken, who,
            intent, requestCode, options);
    ...
}
```

`Instrumentation`的`execStartActivity`代码如下：

```java
public ActivityResult execStartActivity(
    Context who, IBinder contextThread, IBinder token, String target,
    Intent intent, int requestCode, Bundle options) {
    ...
    try {
        ...
        int result = ActivityManagerNative.getDefault()
            .startActivity(whoThread, who.getBasePackageName(), intent,
                    intent.resolveTypeIfNeeded(who.getContentResolver()),
                    token, target, requestCode, 0, null, options);
        checkStartActivityResult(result, intent);
    } catch (RemoteException e) {
        throw new RuntimeException("Failure from system", e);
    }
    return null;
}
```

其中调用了`ActivityManagerNative.getDefault()`的`startActivity()`，那么此处`getDefault()`获取到的是什么？接着看代码：

```java
/**
 * Retrieve the system's default/global activity manager.
 */
static public IActivityManager getDefault() {
    // step 1
    return gDefault.get();
}

// step 2
private static final Singleton<IActivityManager> gDefault = new Singleton<IActivityManager>() {
    protected IActivityManager create() {
		// step 5
        IBinder b = ServiceManager.getService("activity");
        if (false) {
            Log.v("ActivityManager", "default service binder = " + b);
        }
        IActivityManager am = asInterface(b);
        if (false) {
            Log.v("ActivityManager", "default service = " + am);
        }
        return am;
    }
};

public abstract class Singleton<T> {
    private T mInstance;

    protected abstract T create();

    // step 3
    public final T get() {
        synchronized (this) {
            if (mInstance == null) {
			    // step 4
                mInstance = create();
            }
            return mInstance;
        }
    }
}
```

`gDefault`是一个`Singleton<IActivityManager>`类型的静态常量，它的`get()`方法返回的是`Singleton`类中的`private T mInstance;`，这个`mInstance`的创建又是在`gDefault`实例化时通过`create()`方法实现。

这里代码有点绕，根据上面代码注释的`step1 ~ 5`，应该能理清楚：`gDefault.get()`获取到的`mInstance`实例就是`ActivityManagerService`（AMS）实例。

由于`gDefault`是一个静态常量，因此可以通过反射获取到它的实例，同时它是`Singleton`类型的，因此可以获取到其中的`mInstance`。

到这里你应该能明白接下来要干什么了吧，没错就是Hook AMS。

**1.2 Hook AMS**

本文以android 6.0代码为例。注：8.0以下实现方式是相同的，8.0和9.0实现相同，10.0到12.0方式是一样的。

这里涉及到反射及动态代理的姿势，请自行了解。

1，获取`gDefault`实例

```java
Class<?> activityManagerNative = Class.forName("android.app.ActivityManagerNative");
Field singletonField = activityManagerNative.getDeclaredField("gDefault");
singletonField.setAccessible(true);
// 获取gDefault实例
Object singleton = singletonField.get(null);
```

2，获取`Singleton`中的`mInstance`

```java
Class<?> singletonClass = Class.forName("android.util.Singleton");
Field mInstanceField = singletonClass.getDeclaredField("mInstance");
mInstanceField.setAccessible(true);
/* Object mInstance = mInstanceField.get(singleton); */
Method getMethod = singletonClass.getDeclaredMethod("get");
Object mInstance = getMethod.invoke(singleton);
```

这里本可以直接通过`mInstance`的`Field`及第一步中获取的`gDefault`实例反射得到`mInstance`实例，但是实测发现在Android 10以上无法获取，不过还好可以通过`Singleton`中的`get()`方法可以获取到其实例。

3，获取要动态代理的Interface

```
Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
```

4，创建一个代理对象

```java
Object proxyInstance = Proxy.newProxyInstance(context.getClassLoader(), new Class[]{iActivityManagerClass},
        (proxy, method, args) -> {
            if (method.getName().equals("startActivity") && !isLogin()) {
                // 拦截逻辑
            }
            return method.invoke(mInstance, args);
        });
```

5，用代理对象替换原mInstance对象

```java
mInstanceField.set(singleton, proxyInstance);
```

6，兼容性

针对8.0以下，8.0到9.0，10.0到12.0进行适配，可以兼容各个系统版本。



至此已经实现了对AMS的Hook，只需要在代理中判断当前要启动的Activity是否需要登录，然后跳转到登录即可。

但是此时出现了一个问题，这里如何判断哪些Activity需要登录的？最简单的方式就是写死，如下：

```java
// 获取要启动的Activity的全类名。
String intentName = xxx
if (intentName.equals("aaaActivity")
    || intentName.equals("bbbActivity")
    ...
    || intentName.equals("xxxActivity")){
    // 去登陆
}
```

这样的代码存在着耦合，添加删除目标Activity都需要改这里。

接下来就是通过APT实现解耦的方案。

#### 2， APT实现解耦

APT就不多说了，就是注解处理器，很多流行框架都在用它，如果你不了解请自行了解。

首先定义注解，然后给目标Activity加上注解就相当于打了个标记，接着通过APT找到打了这些标记的Activity，将其全类名保存起来，最后在需要使用的地方通过反射调用即可。

**2.1，定义注解**

```java
// 目标页面注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RequireLogin {
    // 需要登录的Activity加上该注解
}

// 登录页面注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginActivity {
    // 给登录页面加上该注解，方便在Hook中直接调用
}

// 判断是否登录方法的注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JudgeLogin {
	// 给判断是否登录的方法添加注解，需要是静态方法。
}
```

**2.2，注解处理器**

这里就不贴代码了，重点是思路：

1，获取所有添加了`RequireLogin`注解的Activity，存入一个集合中

2，通过JavaPoet创建一个Class

3，在其中添加方法，返回1中集合里Activity的全类名的List

最终通过APT生成的类文件如下：

```java
package me.wsj.login.apt;

public class AndLoginUtils {
	// 需要登录的Activity的全类名集合
    public static List<String> getNeedLoginList() {
        List<String> result = new ArrayList<>();
        result.add("me.wsj.andlogin.activity.TargetActivity1");
        result.add("me.wsj.andlogin.activity.TargetActivity2");
        return result;
    }
    
	// 登录Activity的全类名
    public static String getLoginActivity() {
    	return "me.wsj.andlogin.activity.LoginActivity";
    }

    // 判断是否登录的方法全类名
    public static String getJudgeLoginMethod() {
    	return "me.wsj.andlogin.activity.LoginActivity#checkLogin";
    }
}
```

**2.3，反射调用**

在动态代理的`InvocationHandler`中通过反射获取

```java
new InvocationHandler() {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("startActivity") && !isLogin()) {
            // 目标Activity全类名
            String intentName = xxx;
            if (isRequireLogin(intentName)) {
                // 该Activity需要登录，跳转到登录页面
            }
        }
    	return null;
    }
}

/**
 * 该activity是否需要登录
 *
 * @param activityName
 * @return
 */
private static boolean isRequireLogin(String activityName) {
    if (requireLoginNames.size() == 0) {
        // 反射调用apt生成的方法
        try {
            Class<?> NeedLoginClazz = Class.forName(UTILS_PATH);
            Method getNeedLoginListMethod = NeedLoginClazz.getDeclaredMethod("getRequireLoginList");
            getNeedLoginListMethod.setAccessible(true);
            requireLoginNames.addAll((List<String>) getNeedLoginListMethod.invoke(null));
            Log.d("HootUtil", "size" + requireLoginNames.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    return requireLoginNames.contains(activityName);
}
```

**2.4，其他**

实现了判断目标页面的解耦，同样的方式也可以实现跳转登录及判断是否登录的解耦。

1，跳转登录页面

前面定义了`LoginActivity()`注解，APT也生成了`getLoginActivity()`方法，那就可以反射获取到配置的登录Activity，然后创建新的Intent，替换掉原Intent，进而实现跳转到登录页面。

```java
if (需要跳转到登录) {
    Intent intent = new Intent(context, getLoginActivity());
    // 然后需要将该intent替换掉原intent接口
}

/**
 * 获取登录activity
 *
 * @return
 */
private static Class<?> getLoginActivity() {
    if (loginActivityClazz == null) {
        try {
            Class<?> NeedLoginClazz = Class.forName(UTILS_PATH);
            Method getLoginActivityMethod = NeedLoginClazz.getDeclaredMethod("getLoginActivity");
            getLoginActivityMethod.setAccessible(true);
            String loginActivity = (String) getLoginActivityMethod.invoke(null);
            loginActivityClazz = Class.forName(loginActivity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    return loginActivityClazz;
}
```

2，判断是否登录

同理为了实现对判断是否登录的解耦，在判断是否能登录的方法上添加一个`JudgeLogin`注解，就可以在Hook中反射调用判断。



**2.5，小结**

通过APT实现了对判断是否登录、判断哪些页面需要登录及跳转登录的解耦。

此时面临着最后一个问题，虽然前面已经实现了拦截并跳转到了登录页面，但是登录完成后再返回到原页面看似合理，实则不XXXX(词穷了，自行脑补😂)，用户的意图被打断了。

接着就看看如何在登录成功后继续用户意图。



#### 3， 继续用户意图

由于Intent实现了`Parcelable`接口，因此可以将它作为一个Intent的Extra参数传递。在Hook过程中可以获取原始Intent，因此只需在Hook中将用户的原始意图Intent作为一个附加参数存入跳转登录的Intent中，然后在登录页面获取到这个参数，登录成功后跳转到这个原始Intent即可。

1，传递原始意图

在动态代理中先拿到原始Intent，然后将它作为参数存入新的Intent中

```java
new InvocationHandler() {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("startActivity") && !isLogin()) {
            // 目标Activity全类名
            Intent originIntent = xxx;
            String intentName = xxx;
            if (isRequireLogin(intentName)) {
                // 该Activity需要登录，跳转到登录页面
                Intent intent = new Intent(context, getLoginActivity());
                intent.putExtra(Constant.Hook_AMS_EXTRA_NAME, originIntent);
               	// 然后替换原Intent
                ...
            }
        }
    	return null;
    }
}
```

2，获取原始意图并跳转

在登录页面，登录成功后判断其intent中是否有特定键值的附加数据，如果有则直接用它作为意图启动新页面，实现了继续用户意图的目的；

```kotlin
@LoginActivity
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ...
        binding.btnLogin.setOnClickListener {
          	// 登录成功了
            var targetIntent = intent.getParcelableExtra<Intent>(AndLogin.TARGET_ACTIVITY_NAME)
            // 如果存在targetIntent则启动目标intent
            if (targetIntent != null) {
                startActivity(targetIntent)
            }
            finish()
        }
    }

    companion object {
        // 该方法用于返回是否登录
        @JudgeLogin
        @JvmStatic
        fun checkLogin(): Boolean {
            return SpUtil.isLogin()
        }
    }
}
```

如上所示，如果可以在当前Intent中获取到Hook时保存的数据，则说明存在目标Intent，只需将其启动即可。

看一下最终效果：

![preview](/img/preview.gif)

#### 4， ARouter方案

熟悉ARouter的都知道，它有一个拦截器的东西，可以在跳转前做拦截操作。如下：

```java
@Interceptor(name = "login", priority = 1)
public class LoginInterceptorImpl implements IInterceptor {
    @Override
    public void process(Postcard postcard, InterceptorCallback callback) {
		...
        if (isLogin) { // 已经登录不拦截
            callback.onContinue(postcard);
        } else {  // 未登录则拦截
            // callback.onInterrupt(null);
        }
    }

    @Override
    public void init(Context context) {
    }
}
```

实现`IInterceptor`接口并添加`Interceptor`注解即可在路由跳转时实现拦截。

了解其原理的话可知：ARouter也只是在启动Activity前提供了拦截判断的时机，相当于本方案的第一步（Hook AMS）操作，后续实现解耦以及继续用户意图操作还需要自己实现。



#### 5， 总结

本文提出了一种**通过Hook AMS + APT实现集中式登录的方案**，对比传统方式本方案存在以下优势：

1. 以非侵入性的方式将分散的登录判断逻辑集中处理，减少了代码量，提高了开发效率。

2. 增加或删除目标页面时无需修改判断逻辑，只需增加或删除其对应注解即可，符合开闭原则，降低了耦合度
3. 在用户登录成功后直接跳转到目标界面，保证了用户操作不被中断。

**本方案并没有太高深的东西，只是把常用的东西整合在一起，综合运用了一下**。另外方案只是针对需要跳转页面的情况，对于判断是否登录后做其他操作的，比如弹出一个Toast这样的操作，可以通过AspectJ等来实现。

项目地址：https://github.com/wdsqjq/AndLogin

最后，本方案提供了远程依赖，使用startup实现了无侵入初始化，使用方式如下：

1，添加依赖

```groovy
allprojects {
    repositories {
        maven { url 'https://www.jitpack.io' }
    }
}


dependencies {
	implementation 'com.github.wdsqjq.AndLogin:lib:1.0.0'
	kapt 'com.github.wdsqjq.AndLogin:apt_processor:1.0.0'
}
```

2，给需要登录的Activity添加注解

```kotlin
@RequireLogin
class TargetActivity1 : AppCompatActivity() {
	...
}

@RequireLogin
class TargetActivity2 : AppCompatActivity() {
	...
}
```

3，给登录Activity添加注解

```java
@LoginActivity
class LoginActivity : AppCompatActivity() {
	...
}
```

4，提供判断是否登录的方法

需要是一个静态方法

```java
@LoginActivity
class LoginActivity : AppCompatActivity() {

    companion object {
        // 该方法用于返回是否登录
        @JudgeLogin
        @JvmStatic
        fun checkLogin(): Boolean {
            return SpUtil.isLogin()
        }
    }
}
```



参考：

https://github.com/Xiasm/LoginArchitecture

https://juejin.cn/post/6844903657817767943

当非登录态时启动登录界面登录成功以后自动帮用户继续之前被打断的操作

AOP中继续其他操作

https://juejin.cn/post/6844903630621917198





Arouter IInterceptor拦截器实现登录

https://www.jianshu.com/p/6f2a99607440