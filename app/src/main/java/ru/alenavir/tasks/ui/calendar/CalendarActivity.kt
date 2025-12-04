package ru.alenavir.tasks.ui.calendar

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.alenavir.tasks.R
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.dto.TaskDto
import ru.alenavir.tasks.data.dto.enums.Status
import ru.alenavir.tasks.ui.profile.ProfileActivity
import ru.alenavir.tasks.ui.setting.SetActivity
import ru.alenavir.tasks.ui.task.AddTaskActivity
import ru.alenavir.tasks.ui.task.UpdateTaskActivity
import ru.alenavir.tasks.ui.tasks.TaskActivity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

@RequiresApi(Build.VERSION_CODES.O)
class CalendarActivity : AppCompatActivity() {

    private val api by lazy { RetrofitClient.createTaskApi(this) }
    private val categoryColors = mutableMapOf<Int, String>()

    private lateinit var pendingLayout: LinearLayout
    private lateinit var doneLayout: LinearLayout
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var calendarView: CalendarView

    private var selectedDate: LocalDate = LocalDate.now()

    // -------------------------
    // ActivityResult для AddTaskActivity
    // -------------------------
    private val addTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Когда возвращаемся из AddTaskActivity, перезагружаем задачи для выбранной даты
        loadTasksForSelectedDate(selectedDate)
    }

    // -------------------------
    // ActivityResult для UpdateTaskActivity
    // -------------------------
    private val updateTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Когда возвращаемся из UpdateTaskActivity, перезагружаем задачи для выбранной даты
        loadTasksForSelectedDate(selectedDate)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        pendingLayout = findViewById(R.id.layoutTasksPending)
        doneLayout = findViewById(R.id.layoutTasksDone)
        fabAddTask = findViewById(R.id.fabAddTask)
        calendarView = findViewById(R.id.calendarView)

        ViewCompat.setOnApplyWindowInsetsListener(calendarView) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        // Цвет фона календаря
        calendarView.setBackgroundColor(Color.parseColor("#1E1E1E"))

        val today = Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        calendarView.date = today.time

        loadTasksForSelectedDate(selectedDate)

        // FAB добавления задачи
        fabAddTask.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            intent.putExtra("selectedDate", selectedDate.toString())
            addTaskLauncher.launch(intent)
        }

        // Выбор даты в календаре
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            loadTasksForSelectedDate(selectedDate)
        }

        setupBottomNavigation()
    }


    private fun loadTasksForSelectedDate(date: LocalDate) {
        val dateStr = date.toString() // yyyy-MM-dd
        lifecycleScope.launch {
            try {
                val response = api.getTasksByDate(dateStr)
                if (response.isSuccessful) {
                    displayTasks(response.body() ?: emptyList())
                } else {
                    Log.e("CalendarActivity", "Ошибка загрузки задач: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("CalendarActivity", "Ошибка запроса задач", e)
            }
        }
    }

    private fun displayTasks(tasks: List<TaskDto>) {
        pendingLayout.removeAllViews()
        doneLayout.removeAllViews()

        for (task in tasks) {
            val taskView = layoutInflater.inflate(R.layout.item_task, null) as MaterialCardView
            val catColorHex = task.categoryId?.let { categoryColors[it] } ?: "#454545"
            taskView.setCardBackgroundColor(Color.parseColor(catColorHex))

            val checkBox = taskView.findViewById<android.widget.CheckBox>(R.id.checkTaskDone)
            val textTitle = taskView.findViewById<android.widget.TextView>(R.id.textTaskTitle)
            val textDate = taskView.findViewById<android.widget.TextView>(R.id.textTaskDate)
            val buttonDelete = taskView.findViewById<ImageButton>(R.id.buttonDeleteTask)

            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            textTitle.text = task.title
            textDate.text = task.date.format(formatter)
            checkBox.isChecked = task.status == Status.DONE

            // Клик на карточку открывает UpdateTaskActivity через launcher
            taskView.setOnClickListener {
                val intent = Intent(this, UpdateTaskActivity::class.java)
                intent.putExtra(UpdateTaskActivity.EXTRA_TASK_ID, task.id)
                updateTaskLauncher.launch(intent)
            }

            // Удаление задачи
            buttonDelete.setOnClickListener {
                lifecycleScope.launch {
                    try {
                        val response = api.deleteTask(task.id!!)
                        if (response.isSuccessful) {
                            displayTasks(tasks.filter { it.id != task.id })
                        } else {
                            Log.e("CalendarActivity", "Ошибка удаления задачи: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("CalendarActivity", "Ошибка запроса на удаление", e)
                    }
                }
            }

            // Изменение статуса задачи
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

            if (task.status == Status.DONE) {
                doneLayout.visibility = android.view.View.VISIBLE
                doneLayout.addView(taskView)
            } else {
                pendingLayout.visibility = android.view.View.VISIBLE
                pendingLayout.addView(taskView)
            }
        }

        if (pendingLayout.childCount == 0) pendingLayout.visibility = android.view.View.GONE
        if (doneLayout.childCount == 0) doneLayout.visibility = android.view.View.GONE
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_calendar

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    startActivity(Intent(this, TaskActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_calendar -> {
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

