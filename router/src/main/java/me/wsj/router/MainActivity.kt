package me.wsj.router

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.launcher.ARouter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ARouter.openLog()
        ARouter.openDebug()
        ARouter.init(application)

        findViewById<Button>(R.id.btn).setOnClickListener {
//            startActivity(Intent(this, MainActivity2::class.java))
            ARouter.getInstance().build("/test/activity2").navigation()
        }
    }
}