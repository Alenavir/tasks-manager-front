package ru.alenavir.tasks.ui.category

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.colorChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.alenavir.tasks.R
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.dto.CategoryDto
import ru.alenavir.tasks.data.dto.enums.Priority
import ru.alenavir.tasks.databinding.ActivityUpdateCategoryBinding

class UpdateCategoryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CATEGORY_ID = "category_id"
    }

    private lateinit var binding: ActivityUpdateCategoryBinding
    private val api = RetrofitClient.createCategoryApi(this)
    private var selectedColor: String = "#ddc5a2"
    private var categoryId: Int? = null
    private var currentCategory: CategoryDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Прозрачный фон и dim
        window.setFlags(
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        )

        binding = ActivityUpdateCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем ID категории из intent
        categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, -1)
        if (categoryId == -1) {
            Toast.makeText(this, "Ошибка: категория не найдена", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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

        // Загружаем данные категории
        loadCategory()

        // Кнопка "Обновить"
        binding.buttonCreateCategory.setOnClickListener {
            val name = binding.editCategoryName.text.toString()
            if (name.isBlank()) {
                Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentCategory?.let { category ->
                val updatedCategory = category.copy(
                    name = name,
                    color = selectedColor
                )
                updateCategory(updatedCategory)
            }
        }

        // Кнопка "Отменить"
        binding.buttonCancel.setOnClickListener {
            finish()
        }

        // Кнопка "Удалить"
        binding.buttonDeleteCategory.setOnClickListener {
            currentCategory?.let { category ->
                deleteCategory(category.id!!)
            }
        }
    }

    private fun loadCategory() {
        categoryId?.let { id ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val response = api.getCategoryById(id)
                    if (response.isSuccessful) {
                        currentCategory = response.body()
                        currentCategory?.let { category ->
                            binding.editCategoryName.setText(category.name)
                            selectedColor = category.color
                            binding.tvChooseColor.setBackgroundColor(Color.parseColor(category.color))
                        }
                    } else {
                        Toast.makeText(this@UpdateCategoryActivity, "Ошибка загрузки категории", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@UpdateCategoryActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun updateCategory(category: CategoryDto) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = api.updateCategory(category.id!!, category)
                if (response.isSuccessful) {
                    Toast.makeText(this@UpdateCategoryActivity, "Категория обновлена", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@UpdateCategoryActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@UpdateCategoryActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteCategory(id: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = api.deleteCategoryById(id)
                if (response.isSuccessful) {
                    Toast.makeText(this@UpdateCategoryActivity, "Категория удалена", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@UpdateCategoryActivity, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@UpdateCategoryActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
