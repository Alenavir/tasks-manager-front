package ru.alenavir.tasks.ui.register

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.dto.RegistrationDto
import ru.alenavir.tasks.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    val api = RetrofitClient.createAuthApi(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonRegister.setOnClickListener {
            register()
        }
    }

    private fun register() {
        val dto = RegistrationDto(
            username = binding.editEmail.text.toString(),
            password = binding.editPassword.text.toString(),
            name = binding.editName.text.toString(),
            nick = binding.editNick.text.toString()
        )

        val url = RetrofitClient.BASE_URL + "/api/users/registration"
        Log.d("RegisterActivity", "Отправка запроса на: $url")
        Log.d("RegisterActivity", "Данные: $dto")


        lifecycleScope.launch {
            val response = api.register(dto)
            if (response.isSuccessful) {
                Toast.makeText(this@RegisterActivity, "OK", Toast.LENGTH_LONG).show()
                finish()
            } else {
                val errorBody = response.errorBody()?.string()
                Toast.makeText(this@RegisterActivity, "Ошибка: ${response.code()} \n$errorBody", Toast.LENGTH_LONG).show()
            }
        }
    }
}
