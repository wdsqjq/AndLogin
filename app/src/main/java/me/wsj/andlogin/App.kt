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

    override fun onCreate() {
        super.onCreate()

        mContext = this
    }
}