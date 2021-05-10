package me.wsj.andlogin.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.wsj.andlogin.databinding.ActivityMainBinding
import me.wsj.andlogin.utils.SpUtil

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btn1.setOnClickListener {
            startActivity(Intent(this, TargetActivity1::class.java))
        }

        binding.btn2.setOnClickListener {
            startActivity(Intent(this, TargetActivity2::class.java))
        }

        binding.btn3.setOnClickListener {
            startActivity(Intent(this, TargetActivity3::class.java))
        }

        binding.btn4.setOnClickListener {
            SpUtil.logout()
        }
    }
}