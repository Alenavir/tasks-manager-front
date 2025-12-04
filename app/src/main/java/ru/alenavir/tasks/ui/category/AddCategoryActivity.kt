package ru.alenavir.tasks.ui.category

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.alenavir.tasks.R
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.dto.CategoryDto
import ru.alenavir.tasks.data.dto.enums.Priority
import ru.alenavir.tasks.databinding.ActivityAddCategoryBinding

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCategoryBinding
    val api = RetrofitClient.createCategoryApi(this)
    private var selectedColor: String =  "#888888"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvChooseColor.setOnClickListener {

            val colors = intArrayOf(
                Color.parseColor("#2B360E"),
                Color.parseColor("#282E0A"),
                Color.parseColor("#818C50"),
                Color.parseColor("#546D5A")
            )

            // Создаем горизонтальный контейнер для кружков
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.CENTER
            }

            val dialog = AlertDialog.Builder(this)
                .setTitle("Выберите цвет")
                .setView(container)
                .create()

            // Добавляем кружки
            colors.forEach { color ->
                val circle = TextView(this).apply {
                    text = "\u25CF"
                    textSize = 50f
                    setTextColor(color)
                    gravity = Gravity.CENTER
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(16, 0, 16, 0)
                    layoutParams = params

                    setOnClickListener {
                        selectedColor = String.format("#%06X", 0xFFFFFF and color)
                        binding.tvChooseColor.setBackgroundColor(color)
                        dialog.dismiss()
                    }
                }
                container.addView(circle)
            }

            dialog.setOnShowListener {
                // Черный фон диалога
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#292929")))

                // Цвет и центрирование заголовка
                val titleId = androidx.appcompat.R.id.alertTitle
                val title = dialog.findViewById<TextView>(titleId)
                title?.apply {
                    setTextColor(Color.parseColor("#F2F7F7"))
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }

                // Центрируем родительский LinearLayout заголовка
                (title?.parent as? LinearLayout)?.gravity = Gravity.CENTER_HORIZONTAL
            }

            dialog.show()
        }

        // Кнопка "Создать"
        binding.buttonCreateCategory.setOnClickListener {
            val name = binding.editCategoryName.text.toString()
            if (name.isBlank()) {
                Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val dto = CategoryDto(name = name, color = selectedColor, priority = Priority.LOW)

            // Вызов API
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val response = api.createCategory(dto)
                    if (response.isSuccessful) {
                        Toast.makeText(this@AddCategoryActivity, "Категория создана", Toast.LENGTH_SHORT).show()
                        finish() // Закрываем окно
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val code = response.code()
                        Toast.makeText(this@AddCategoryActivity, "Ошибка создания", Toast.LENGTH_SHORT).show()
                        Log.e("ActivityAddCategory", "Ошибка создания категории. Код: $code, тело: $errorBody")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@AddCategoryActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.buttonCancel.setOnClickListener {
            finish() // Закрываем текущую Activity и возвращаемся на TaskActivity
        }
    }

}
