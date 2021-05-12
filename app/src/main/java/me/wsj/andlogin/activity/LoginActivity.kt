package me.wsj.andlogin.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.wsj.andlogin.databinding.ActivityLoginBinding
import me.wsj.andlogin.utils.SpUtil
import me.wsj.login.apt.AndLoginUtils
import me.wsj.login.hook.AndLogin
import per.wsj.annotation.JudgeLogin
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
//            AndLogin.getInstance().handleIntent()
            var targetIntent = intent.getParcelableExtra<Intent>(AndLogin.TARGET_ACTIVITY_NAME)
            if (targetIntent != null) {
                startActivity(targetIntent)
            }
            finish()
        }
    }

    companion object {
        // 该方法用于返回是否登录
        @JudgeLogin
        @JvmStatic
        fun checkLogin(): Boolean {
            return SpUtil.isLogin()
        }
    }
}