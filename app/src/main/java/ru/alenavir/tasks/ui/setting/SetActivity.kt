package ru.alenavir.tasks.ui.setting

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import ru.alenavir.tasks.R
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.api.UserApi
import ru.alenavir.tasks.data.dto.UserDto
import ru.alenavir.tasks.ui.calendar.CalendarActivity
import ru.alenavir.tasks.ui.login.LoginActivity
import ru.alenavir.tasks.ui.profile.ProfileActivity
import ru.alenavir.tasks.ui.tasks.TaskActivity

class SetActivity : AppCompatActivity() {

    private val userApi: UserApi by lazy { RetrofitClient.createUserApi(this) }

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        editName = findViewById(R.id.editTextName)
        editEmail = findViewById(R.id.editTextEmail)
        buttonSave = findViewById(R.id.buttonSave)
        buttonLogout = findViewById(R.id.buttonLogout)

        loadUser()

        buttonSave.setOnClickListener {
            val newName = editName.text.toString().trim()
            if (newName.isNotEmpty()) updateUserName(newName)
        }

        buttonLogout.setOnClickListener {
            logout()
        }

        setupBottomNavigation()
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val response = userApi.getById()
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        editName.setText(user.name)
                        editEmail.setText(user.username)
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Ошибка загрузки пользователя", e)
            }
        }
    }

    private fun updateUserName(newName: String) {
        lifecycleScope.launch {
            try {
                val currentUser = userApi.getById().body()
                if (currentUser != null) {
                    val updated = UserDto(
                        username = currentUser.username,
                        password = currentUser.password,
                        name = newName,
                        nick = currentUser.nick
                    )
                    val response = userApi.update(updated)
                    if (response.isSuccessful) {
                        Log.d("SettingsActivity", "Имя успешно обновлено")
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Ошибка обновления имени", e)
            }
        }
    }

    private fun logout() {
        // Очистка данных пользователя / токенов здесь, если нужно
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_settings // текущая активная вкладка

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    startActivity(Intent(this, TaskActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    true
                }
                else -> false
            }
        }
    }
}