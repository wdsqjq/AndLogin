package me.wsj.login.startup

import android.content.Context
import androidx.startup.Initializer
import me.wsj.login.hook.AndLogin

class AndLoginInitializer : Initializer<Unit> {
    override fun create(context: Context) {
//        CrashHandler.getInstance().init(context)
        AndLogin.getInstance().init(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}