package me.wsj.andlogin

import android.app.Application
import android.content.Context
import me.wsj.andlogin.utils.SpUtil
import per.wsj.annotation.JudgeLogin

class App : Application() {

    companion object {
        @JvmStatic
        lateinit var mContext: Context
    }

    // 该方法用于返回是否登录，可以放置在任何位置
    @JudgeLogin
    fun checkLogin(): Boolean {
        return SpUtil.isLogin()
    }

    override fun onCreate() {
        super.onCreate()

        mContext = this
    }
}