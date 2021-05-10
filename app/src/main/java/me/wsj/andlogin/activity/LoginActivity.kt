package me.wsj.andlogin.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.wsj.andlogin.databinding.ActivityLoginBinding
import me.wsj.andlogin.utils.SpUtil
import per.wsj.annotation.LoginActivity

@LoginActivity
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.login.setOnClickListener {
            SpUtil.login(binding.username.text.toString())
        }
    }
}