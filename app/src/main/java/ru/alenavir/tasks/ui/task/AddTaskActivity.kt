package ru.alenavir.tasks.ui.task

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.alenavir.tasks.R
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.dto.CategoryDto
import ru.alenavir.tasks.data.dto.TaskDto
import ru.alenavir.tasks.data.dto.enums.Priority
import ru.alenavir.tasks.data.dto.enums.Status
import ru.alenavir.tasks.databinding.ActivityAddTaskBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding

    private val apiCategory by lazy { RetrofitClient.createCategoryApi(this) }
    private val apiTask by lazy { RetrofitClient.createTaskApi(this) }

    // Поля формы
    private lateinit var layoutTitle: TextInputLayout
    private lateinit var editTitle: TextInputEditText
    private lateinit var layoutDescription: TextInputLayout
    private lateinit var editDescription: TextInputEditText
    private lateinit var categoryField: MaterialAutoCompleteTextView
    private lateinit var priorityField: MaterialAutoCompleteTextView

    private lateinit var selectedDate: LocalDate
    private var categoriesList = listOf<CategoryDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем дату задачи из интента
        val dateStr = intent.getStringExtra("selectedDate") ?: LocalDate.now().toString()
        selectedDate = LocalDate.parse(dateStr)

        initViews()
        setupPriorityField()
        loadCategories()

        binding.buttonCreateCategory.setOnClickListener { createTask() }
        binding.buttonCancel.setOnClickListener { finish() }
    }

    private fun initViews() {
        layoutTitle = binding.filledName
        editTitle = binding.editName
        layoutDescription = binding.layoutTaskDescription
        editDescription = binding.editTaskDescription
        categoryField = binding.spinnerTaskCategory
        priorityField = binding.spinnerPriority
    }

    private fun setupPriorityField() {
        val priorities = listOf("HIGH", "MEDIUM", "LOW")
        val adapter = ArrayAdapter(
            this,
            R.layout.item_dropdown,
            priorities
        )

        priorityField.setAdapter(adapter)

        // Устанавливаем значение по умолчанию
        priorityField.setText("LOW", false)

        // Открытие списка при клике на поле
        priorityField.setOnClickListener { priorityField.showDropDown() }
    }


    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val response: Response<List<CategoryDto>> = apiCategory.getAllCategories()
                if (response.isSuccessful) {
                    categoriesList = response.body() ?: emptyList()

                    // Добавляем "Все" в начало списка
                    val names = listOf("Все") + categoriesList.map { it.name }

                    val adapter = ArrayAdapter(
                        this@AddTaskActivity,
                        R.layout.item_dropdown,
                        names
                    )
                    categoryField.setAdapter(adapter)

                    // Устанавливаем значение по умолчанию
                    categoryField.setText("Все", false)

                    // Открытие списка при клике
                    categoryField.setOnClickListener { categoryField.showDropDown() }

                } else {
                    Toast.makeText(
                        this@AddTaskActivity,
                        "Ошибка загрузки категорий: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("AddTaskActivity", "Ошибка при загрузке категорий", e)
                Toast.makeText(this@AddTaskActivity, "Ошибка при загрузке категорий", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTask() {
        val title = editTitle.text.toString().trim()
        val description = editDescription.text.toString().trim()

        if (title.isEmpty()) {
            layoutTitle.error = "Введите название"
            return
        } else layoutTitle.error = null

        val categoryName = categoryField.text.toString()
        val categoryId: Int? = if (categoryName == "Все") null
        else categoriesList.firstOrNull { it.name == categoryName }?.id

        val priorityText = priorityField.text.toString()
        val priority = when (priorityText) {
            "HIGH" -> Priority.HIGH
            "MEDIUM" -> Priority.MEDIUM
            "LOW" -> Priority.LOW
            else -> Priority.LOW
        }

        val dto = TaskDto(
            title = title,
            description = description,
            categoryId = categoryId,
            status = Status.TODO,
            date = selectedDate.format(DateTimeFormatter.ISO_DATE),
            priority = priority
        )

        lifecycleScope.launch {
            try {
                val response: Response<TaskDto> = apiTask.createTask(dto)
                if (response.isSuccessful) {
                    Toast.makeText(this@AddTaskActivity, "Задача создана!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddTaskActivity, "Ошибка создания задачи: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AddTaskActivity", "Ошибка при создании задачи", e)
                Toast.makeText(this@AddTaskActivity, "Ошибка при создании задачи", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
