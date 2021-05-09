package per.wsj.lib.hook;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class HookUtil {

    private static List<String> needLoginNames = new ArrayList<>();

    /**
     * 不可再Application的onCreate()中直接调用，可以延迟一会，哪怕一毫秒都行，不然mInstance会为空
     *
     * @param context
     */
    public static void HookAms(Context context) {
        try {
            Field singletonField;
            Class<?> iActivityManagerClass;
            // 1，获取Instrumentation中调用startActivity(,intent,)方法的对象
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 10.0是ActivityTaskManager中的IActivityTaskManagerSingleton
                Class<?> activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager");
                singletonField = activityTaskManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
                iActivityManagerClass = Class.forName("android.app.IActivityTaskManager");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 8.0,9.0在ActivityManager类中IActivityManagerSingleton
                Class activityManagerClass = ActivityManager.class;
                singletonField = activityManagerClass.getDeclaredField("IActivityManagerSingleton");
                iActivityManagerClass = Class.forName("android.app.IActivityManager");
            } else {
                // 8.0以下在ActivityManagerNative类中 gDefault
                Class<?> activityManagerNative = Class.forName("android.app.ActivityManagerNative");
                singletonField = activityManagerNative.getDeclaredField("gDefault");
                iActivityManagerClass = Class.forName("android.app.IActivityManager");
            }
            singletonField.setAccessible(true);
            Object singleton = singletonField.get(null);

            // 2，获取Singleton中的mInstance，也就是要代理的对象
            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            Object mInstance = mInstanceField.get(singleton);
            if (mInstance == null) {
                return;
            }
            // 3，对IActivityManager进行动态代理
            Object proxyInstance = Proxy.newProxyInstance(context.getClassLoader(), new Class[]{iActivityManagerClass},
                    (proxy, method, args) -> {
                        /*if (method.getName().equals("startActivity")) {
                            if (!SpUtil.isLogin(context)) {
                                int pos = 0;
                                for (int i = 0; i < args.length; i++) {
                                    if (args[i] instanceof Intent) {
                                        pos = i;
                                        break;
                                    }
                                }
                                Intent originIntent = (Intent) args[pos];
                                if (originIntent.getComponent() != null) {
                                    // 解决：请求权限启动PermissionActivity时异常
                                    String activityName = originIntent.getComponent().getClassName();

                                    if (needLogin(activityName)) {
                                        Intent intent = new Intent(context, LoginActivity.class);
                                        intent.putExtra(Constant.HOOK_AMS_EXTRA_NAME, originIntent);
                                        args[pos] = intent;
                                    }
                                }
                            }
                        }*/
                        return method.invoke(mInstance, args);
                    });
            // 4，把代理赋值给IActivityManager类型的mInstance对象
            mInstanceField.set(singleton, proxyInstance);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean needLogin(String activityName) {
        if (needLoginNames.size() == 0) {
            // 反射调用apt生成的方法
            try {
                Class<?> NeedLoginClazz = Class.forName("per.wsj.gitstar.apt.NeedLogin");
                Method getNeedLoginListMethod = NeedLoginClazz.getDeclaredMethod("getNeedLoginList");
                getNeedLoginListMethod.setAccessible(true);
                Object obj = NeedLoginClazz.newInstance();
                needLoginNames.addAll((List<String>) getNeedLoginListMethod.invoke(obj));
                Log.d("HootUtil", "size" + needLoginNames.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return needLoginNames.contains(activityName);
    }

    public static void test() {
        try {
            Class<?> NeedLoginClazz = Class.forName("per.wsj.login.apt.NeedLogin");
            Method getNeedLoginListMethod = NeedLoginClazz.getDeclaredMethod("getNeedLoginList");
            getNeedLoginListMethod.setAccessible(true);
            Object obj = NeedLoginClazz.newInstance();
            needLoginNames.addAll((List<String>) getNeedLoginListMethod.invoke(obj));
            Log.d("HootUtil", "size: " + needLoginNames.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
