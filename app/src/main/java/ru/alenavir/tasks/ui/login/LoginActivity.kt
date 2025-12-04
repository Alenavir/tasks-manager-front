package ru.alenavir.tasks.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.dto.UserCredentialsDto
import ru.alenavir.tasks.databinding.ActivityLoginBinding
import ru.alenavir.tasks.ui.main.MainActivity
import kotlinx.coroutines.launch
import com.google.android.material.textfield.TextInputEditText
import ru.alenavir.tasks.ui.tasks.TaskActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    val api = RetrofitClient.createAuthApi(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener { login() }
    }

    private fun login() {
        val email = binding.editEmail.text.toString()
        val password = binding.editPassword.text.toString()

        lifecycleScope.launch {
            val response = api.signIn(
                UserCredentialsDto(email, password)
            )

            if (response.isSuccessful) {
                val data = response.body()!!
                TokenStorage.saveTokens(this@LoginActivity, data.accessToken, data.refreshToken)

                // Запуск CategoryActivity вместо MainActivity
                val intent = Intent(this@LoginActivity, TaskActivity::class.java)
                startActivity(intent)

                // Чтобы пользователь не мог вернуться на LoginActivity кнопкой "назад"
                finish()
            } else {
                Toast.makeText(this@LoginActivity, "Ошибка", Toast.LENGTH_LONG).show()
            }

        }
    }
}
