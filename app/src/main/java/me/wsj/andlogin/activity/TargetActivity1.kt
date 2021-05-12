package me.wsj.andlogin.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import me.wsj.andlogin.R
import me.wsj.andlogin.utils.SpUtil
import per.wsj.annotation.NeedLogin

@NeedLogin
class TargetActivity1 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target)

        findViewById<TextView>(R.id.tvAccount).text = "当前用户：" + SpUtil.getAccount()
    }
}