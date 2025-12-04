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
import ru.alenavir.tasks.databinding.ActivityUpdateTaskBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class UpdateTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }

    private lateinit var binding: ActivityUpdateTaskBinding

    private val apiCategory by lazy { RetrofitClient.createCategoryApi(this) }
    private val apiTask by lazy { RetrofitClient.createTaskApi(this) }

    private var categoriesList = listOf<CategoryDto>()
    private var taskId: Int? = null
    private var currentTask: TaskDto? = null
    private lateinit var layoutTitle: TextInputLayout
    private lateinit var editTitle: TextInputEditText
    private lateinit var layoutDescription: TextInputLayout
    private lateinit var editDescription: TextInputEditText
    private lateinit var categoryField: MaterialAutoCompleteTextView
    private lateinit var priorityField: MaterialAutoCompleteTextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId == -1) {
            Toast.makeText(this, "Ошибка: задача не найдена", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupPriorityField()
        loadTask()
        loadCategories()

        binding.buttonUpdateTask.setOnClickListener { updateTaskClick() }
        binding.buttonCancelUpdate.setOnClickListener { finish() }
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
        val adapter = ArrayAdapter(this, R.layout.item_dropdown, priorities)
        priorityField.setAdapter(adapter)
        priorityField.setOnClickListener { priorityField.showDropDown() }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val response: Response<List<CategoryDto>> = apiCategory.getAllCategories()
                if (response.isSuccessful) {

                    // Загружаем список категорий
                    categoriesList = response.body() ?: emptyList()

                    // Делаем список как в AddTaskActivity: "Все" + категории
                    val names = listOf("Все") + categoriesList.map { it.name }

                    val adapter = ArrayAdapter(
                        this@UpdateTaskActivity,
                        R.layout.item_dropdown,
                        names
                    )
                    categoryField.setAdapter(adapter)
                    categoryField.setOnClickListener { categoryField.showDropDown() }

                    // Категория из задачи уже загружена?
                    currentTask?.let { task ->

                        if (task.categoryId == null) {
                            // Если null → ставим "Все"
                            categoryField.setText("Все", false)
                        } else {
                            // Ищем имя категории
                            val name = categoriesList.firstOrNull { it.id == task.categoryId }?.name
                            if (name != null) categoryField.setText(name, false)
                        }
                    }

                } else {
                    Toast.makeText(
                        this@UpdateTaskActivity,
                        "Ошибка загрузки категорий: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("UpdateTaskActivity", "Ошибка при загрузке категорий", e)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTask() {
        val id = taskId ?: return
        lifecycleScope.launch {
            try {
                val response = apiTask.getTaskById(id)
                if (response.isSuccessful) {
                    currentTask = response.body()
                    currentTask?.let { task ->

                        editTitle.setText(task.title)
                        editDescription.setText(task.description ?: "")

                        val priorityStr = when (task.priority) {
                            Priority.HIGH -> "HIGH"
                            Priority.MEDIUM -> "MEDIUM"
                            Priority.LOW -> "LOW"
                        }
                        priorityField.setText(priorityStr, false)

                        // Категория не подставляется здесь — её подставит loadCategories()
                    }

                } else {
                    Toast.makeText(
                        this@UpdateTaskActivity,
                        "Ошибка загрузки задачи",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("UpdateTaskActivity", "Ошибка loadTask", e)
                finish()
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTaskClick() {
        val title = editTitle.text.toString().trim()
        val description = editDescription.text.toString().trim()

        if (title.isEmpty()) {
            layoutTitle.error = "Введите название"
            return
        } else layoutTitle.error = null

        val categoryName = categoryField.text.toString()
        val categoryId = categoriesList.firstOrNull { it.name == categoryName }?.id

        val priorityText = priorityField.text.toString()
        val priority = when (priorityText) {
            "HIGH" -> Priority.HIGH
            "MEDIUM" -> Priority.MEDIUM
            "LOW" -> Priority.LOW
            else -> Priority.LOW
        }

        currentTask?.let { task ->
            val updatedTask = task.copy(
                title = title,
                description = description,
                categoryId = categoryId,
                date = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                priority = priority
            )
            sendUpdateTask(updatedTask)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendUpdateTask(task: TaskDto) {
        lifecycleScope.launch {
            try {
                val response = apiTask.updateTask(task.id!!, task)
                if (response.isSuccessful) {
                    Toast.makeText(this@UpdateTaskActivity, "Задача обновлена!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@UpdateTaskActivity, "Ошибка обновления: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("UpdateTaskActivity", "Ошибка updateTask", e)
                Toast.makeText(this@UpdateTaskActivity, "Ошибка при обновлении", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
