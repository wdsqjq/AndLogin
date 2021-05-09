package me.wsj.andlogin

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import per.wsj.annotation.NeedLogin

@NeedLogin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}