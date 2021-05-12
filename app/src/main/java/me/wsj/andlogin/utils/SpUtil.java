package me.wsj.andlogin.utils;

import android.content.Context;
import android.content.SharedPreferences;

import me.wsj.andlogin.App;

public class SpUtil {

    private static final String SHARED_NAME = "andlogin_sp";

    private static SharedPreferences sharedPreferences;

    static {
        sharedPreferences = App.mContext.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isLogin() {
        String account = sharedPreferences.getString("account", "");
        return !account.isEmpty();
    }

    public static String getAccount() {
        String account = sharedPreferences.getString("account", "");
        return account;
    }

    public static void login(String account) {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString("account", account);
        edit.commit();
    }

    public static void logout() {
        sharedPreferences.edit().clear().commit();
    }
}
