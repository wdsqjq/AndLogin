package me.wsj.login.hook;

import android.content.Context;

import me.wsj.login.utils.LogUtil;

public class AndLogin {

    private static AndLogin instance;

    private AndLogin() {
    }

    public static AndLogin getInstance() {
        if (instance == null) {
            instance = new AndLogin();
        }
        return instance;
    }

    public void init(Context context) {
//        LogUtil.d("init");
        HookUtil.HookAms(context);
    }
}
