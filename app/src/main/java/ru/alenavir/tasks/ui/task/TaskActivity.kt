package ru.alenavir.tasks.ui.tasks

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.alenavir.tasks.R
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.dto.TaskDto
import ru.alenavir.tasks.data.dto.enums.Priority
import ru.alenavir.tasks.data.dto.enums.Status
import ru.alenavir.tasks.ui.calendar.CalendarActivity
import ru.alenavir.tasks.ui.category.AddCategoryActivity
import ru.alenavir.tasks.ui.category.UpdateCategoryActivity
import ru.alenavir.tasks.ui.profile.ProfileActivity
import ru.alenavir.tasks.ui.setting.SetActivity
import ru.alenavir.tasks.ui.task.AddTaskActivity
import ru.alenavir.tasks.ui.task.UpdateTaskActivity
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

class TaskActivity : AppCompatActivity() {

    private val apiCategory by lazy { RetrofitClient.createCategoryApi(this) }
    private val api by lazy { RetrofitClient.createTaskApi(this) }

    private val categoryColors = mutableMapOf<Int, String>() // id -> цвет категории

    private lateinit var includedCategoryLayout: View
    private lateinit var buttonCategoriesAction: ImageButton
    private lateinit var buttonSortTasks: MaterialButton
    private lateinit var chipGroupCategories: ChipGroup
    private var allChip: Chip? = null

    private var isPriorityFilter: Boolean? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        // Инициализация views
        includedCategoryLayout = findViewById(R.id.include_categories)
        buttonCategoriesAction = includedCategoryLayout.findViewById(R.id.buttonCategoriesAction)
        chipGroupCategories = includedCategoryLayout.findViewById(R.id.layoutCategories)

        // Отступ под статус бар
        ViewCompat.setOnApplyWindowInsetsListener(includedCategoryLayout) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        // Кнопка добавления категории
        buttonCategoriesAction.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        // FAB добавления задачи
        findViewById<View>(R.id.fabAddTask).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        buttonSortTasks = findViewById(R.id.buttonSortTasks)

        buttonSortTasks.setOnClickListener {
            buttonSortTasks.isSelected = !buttonSortTasks.isSelected

            buttonSortTasks.setTextColor(
                if (buttonSortTasks.isSelected)
                    Color.parseColor("#C5E52D")
                else
                    Color.parseColor("#F2F7F7")
            )

            buttonSortTasks.backgroundTintList = ColorStateList.valueOf(
                if (buttonSortTasks.isSelected)
                    Color.parseColor("#10100F")
                else
                    Color.parseColor("#10100F")
            )

            val selectedCategoryId = getSelectedCategoryId()
            loadTasksForCategory(selectedCategoryId)
        }



        loadCategories()
        loadTasksForToday()
        setupBottomNavigation()
    }

    private fun getSelectedCategoryId(): Int? {
        for (i in 0 until chipGroupCategories.childCount) {
            val chip = chipGroupCategories.getChildAt(i) as Chip
            if (chip.isChecked && chip.text != "Все") {
                // ищем id категории по цвету или другому tag
                return categoryColors.entries.firstOrNull { it.value == chip.tag }?.key
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadCategories()
        loadTasksForToday()
    }

    // ------------------------------
    // Загрузка задач за сегодня
    // ------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTasksForToday() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        lifecycleScope.launch {
            try {
                val response = api.getTasksByDate(today)
                if (response.isSuccessful) {
                    displayTasks(response.body() ?: emptyList())
                } else {
                    Log.e("TaskActivity", "Ошибка автозагрузки задач: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("TaskActivity", "Ошибка при автозапросе задач", e)
            }
        }
    }

    // ------------------------------
    // Загрузка категорий
    // ------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadCategories() {
        lifecycleScope.launch {
            val response = apiCategory.getAllCategories()
            if (!response.isSuccessful) {
                Log.e("TaskActivity", "Ошибка загрузки категорий: ${response.code()}")
                return@launch
            }

            val categories = response.body() ?: emptyList()
            chipGroupCategories.removeAllViews()

            // Чип "Все"
            allChip = Chip(this@TaskActivity).apply {
                text = "Все"
                setTextColor(Color.WHITE)
                chipBackgroundColor = ColorStateList.valueOf(Color.DKGRAY)
                isClickable = true
                isCheckable = true
                isChecked = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(8, 8, 8, 8) }

                setOnClickListener {
                    activateChip(this)
                    loadTasksForCategory(null)
                }
            }
            chipGroupCategories.addView(allChip)

            // Остальные категории
            for (cat in categories) {
                cat.id?.let { id ->
                    categoryColors[id] = cat.color ?: "#888888"
                }

                val chip = Chip(this@TaskActivity).apply {
                    text = cat.name
                    setTextColor(Color.WHITE)
                    val chipColor = categoryColors[cat.id] ?: "#888888"
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(chipColor))
                    tag = chipColor
                    isClickable = true
                    isCheckable = true
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(8, 8, 8, 8) }

                    setOnClickListener {
                        activateChip(this)
                        loadTasksForCategory(cat.id)
                    }

                    setOnLongClickListener {
                        val intent = Intent(this@TaskActivity, UpdateCategoryActivity::class.java)
                        intent.putExtra(UpdateCategoryActivity.EXTRA_CATEGORY_ID, cat.id)
                        startActivity(intent)
                        true
                    }
                }
                chipGroupCategories.addView(chip)
            }

            allChip?.performClick()
        }
    }

    // ------------------------------
    // Подсветка активного чипа
    // ------------------------------
    private fun activateChip(selectedChip: Chip) {
        for (i in 0 until chipGroupCategories.childCount) {
            val chip = chipGroupCategories.getChildAt(i) as Chip
            val catColorHex = chip.tag as? String ?: "#888888"
            val catColor = Color.parseColor(catColorHex)

            if (chip == selectedChip) {
                chip.chipBackgroundColor = ColorStateList.valueOf(darkenColor(catColor))
                chip.isChecked = true
            } else {
                chip.chipBackgroundColor = ColorStateList.valueOf(catColor)
                chip.isChecked = false
            }
        }
    }

    private fun darkenColor(color: Int, factor: Float = 0.8f): Int {
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.rgb(r, g, b)
    }

    // ------------------------------
    // Загрузка задач для выбранной категории
    // ------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadTasksForCategory(categoryId: Int?) {
        lifecycleScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                Log.d("TaskActivity", "Загрузка задач для категории: $categoryId на дату $today")

                val isPriority: Boolean? = if (buttonSortTasks.isSelected) true else null

                val response = if (categoryId == null) {
                    api.getTasksByDate(
                        today,
                        null,
                        isPriority
                    )
                } else {
                    api.getTasksByDate(today, categoryId, isPriority)
                }

                if (response.isSuccessful) {
                    displayTasks(response.body() ?: emptyList())
                } else {
                    Log.e("TaskActivity", "Ошибка загрузки задач: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("TaskActivity", "Ошибка запроса задач", e)
            }
        }
    }

    // ------------------------------
    // Отображение задач
    // ------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayTasks(tasks: List<TaskDto>) {
        val pendingLayout = findViewById<LinearLayout>(R.id.layoutTasksPending)
        val doneLayout = findViewById<LinearLayout>(R.id.layoutTasksDone)

        pendingLayout.removeAllViews()
        doneLayout.removeAllViews()

        for (task in tasks) {
            val taskView = layoutInflater.inflate(R.layout.item_task, null) as MaterialCardView
            val catColorHex = task.categoryId?.let { categoryColors[it] } ?: "#888888"

            // Цвет карточки без серого наложения
            taskView.setCardBackgroundColor(Color.parseColor(catColorHex))

            val checkBox = taskView.findViewById<CheckBox>(R.id.checkTaskDone)
            val textTitle = taskView.findViewById<TextView>(R.id.textTaskTitle)
            val textDate = taskView.findViewById<TextView>(R.id.textTaskDate)
            val buttonDelete = taskView.findViewById<ImageButton>(R.id.buttonDeleteTask)

            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            textTitle.text = task.title
            textDate.text = task.date.format(formatter)
            checkBox.isChecked = task.status == Status.DONE

            // Клик на всю карточку открывает UpdateTaskActivity
            taskView.setOnClickListener {
                val intent = Intent(this, UpdateTaskActivity::class.java)
                intent.putExtra(UpdateTaskActivity.EXTRA_TASK_ID, task.id)
                startActivity(intent)
            }

            // Удаление задачи
            buttonDelete.setOnClickListener {
                lifecycleScope.launch {
                    try {
                        val response = api.deleteTask(task.id!!)
                        if (response.isSuccessful) {
                            displayTasks(tasks.filter { it.id != task.id })
                        } else {
                            Log.e("TaskActivity", "Ошибка удаления задачи: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("TaskActivity", "Ошибка запроса на удаление", e)
                    }
                }
            }

            // Изменение статуса
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                lifecycleScope.launch {
                    try {
                        val response: Response<Boolean> = if (isChecked) {
                            api.setTaskDone(task.id!!)
                        } else {
                            api.setTaskToDo(task.id!!)
                        }

                        if (response.isSuccessful && response.body() == true) {
                            task.status = if (isChecked) Status.DONE else Status.TODO
                            displayTasks(tasks)
                        } else {
                            checkBox.isChecked = !isChecked
                        }
                    } catch (e: Exception) {
                        checkBox.isChecked = !isChecked
                    }
                }
            }

            // Добавление в соответствующий контейнер
            if (task.status == Status.DONE) {
                doneLayout.visibility = View.VISIBLE
                doneLayout.addView(taskView)
            } else {
                pendingLayout.visibility = View.VISIBLE
                pendingLayout.addView(taskView)
            }
        }

        if (pendingLayout.childCount == 0) pendingLayout.visibility = View.GONE
        if (doneLayout.childCount == 0) doneLayout.visibility = View.GONE
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_tasks // текущая активная вкладка

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    // уже на этой странице, ничего не делаем
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
                    startActivity(Intent(this, SetActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

}
