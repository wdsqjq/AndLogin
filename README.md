# AndLogin

**通过Hook AMS + APT实现集中式登录**

效果如下：

![preview](/img/preview.gif)



**优势：**

1. 以非侵入性的方式将分散的登录判断逻辑集中处理，减少了代码量，提高了开发效率。
2. 增加或删除目标页面时无需修改判断逻辑，只需增加或删除其对应注解即可，符合开闭原则，降低了耦合度
3. 在用户登录成功后直接跳转到目标界面，保证了用户操作不被中断。

![login](/img/hook_login.png)

**使用：**

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

