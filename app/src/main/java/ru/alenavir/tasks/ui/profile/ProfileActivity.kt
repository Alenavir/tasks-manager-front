package ru.alenavir.tasks.ui.profile

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import ru.alenavir.tasks.R
import ru.alenavir.tasks.data.api.ReportApi
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.api.UserApi
import ru.alenavir.tasks.data.dto.UserDto
import ru.alenavir.tasks.data.dto.enums.Status
import ru.alenavir.tasks.ui.calendar.CalendarActivity
import ru.alenavir.tasks.ui.setting.SetActivity
import ru.alenavir.tasks.ui.tasks.TaskActivity
import java.time.YearMonth

@RequiresApi(Build.VERSION_CODES.O)
class ProfileActivity : AppCompatActivity() {

    private val userApi: UserApi by lazy { RetrofitClient.createUserApi(this) }
    private val reportApi: ReportApi by lazy { RetrofitClient.createReportApi(this) }

    private lateinit var editNickname: EditText
    private lateinit var buttonSave: Button
    private lateinit var barChart: BarChart
    private lateinit var bottomNav: BottomNavigationView

    // new: TextViews inside the cards
    private lateinit var textCompletedValue: TextView
    private lateinit var textUncompletedValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        editNickname = findViewById(R.id.editTextNickname)
        buttonSave = findViewById(R.id.buttonSaveNickname)
        barChart = findViewById(R.id.barChartCategories)
        bottomNav = findViewById(R.id.bottom_navigation)

        // init card text views (they exist in your XML)
        textCompletedValue = findViewById(R.id.textCompletedValue)
        textUncompletedValue = findViewById(R.id.textUncompletedValue)

        setupBottomNavigation()
        loadUser()
        loadReport()

        buttonSave.setOnClickListener {
            val newNick = editNickname.text.toString().trim()
            if (newNick.isNotEmpty()) updateNickname(newNick)
        }
    }

    private fun setupBottomNavigation() {
        bottomNav.selectedItemId = R.id.nav_profile
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
                R.id.nav_profile -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SetActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val response = userApi.getById()
                if (response.isSuccessful) {
                    response.body()?.let { editNickname.setText(it.nick) }
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Ошибка загрузки пользователя", e)
            }
        }
    }

    private fun updateNickname(newNick: String) {
        lifecycleScope.launch {
            try {
                val dto = UserDto(
                    username = "",  // если API требует
                    password = "",
                    name = "",
                    nick = newNick
                )
                val response = userApi.update(dto)
                if (response.isSuccessful) Log.d("ProfileActivity", "Ник обновлен")
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Ошибка обновления ника", e)
            }
        }
    }

    private fun loadReport() {
        val month = YearMonth.now().toString() // yyyy-MM
        lifecycleScope.launch {
            try {
                val categoryResponse = reportApi.getTaskCountByCategoryForMonth(month)
                val statusResponse = reportApi.getTaskCountByStatusForMonth(month)

                if (categoryResponse.isSuccessful && statusResponse.isSuccessful) {

                    val categories = categoryResponse.body() ?: emptyList()
                    val categoryData = categories.associate { it.id to it.taskCount }
                    val categoryNames = categories.associate { it.id to it.name }
                    val categoryColors = categories.associate { it.id to Color.parseColor(it.color) }

                    updateBarChart(categoryData, categoryColors, categoryNames)

                    val statusData: Map<Status, Long> = statusResponse.body() ?: emptyMap()
                    val done = statusData[Status.DONE] ?: 0
                    val todo = statusData[Status.TODO] ?: 0


                    // put values directly into the card TextViews
                    textCompletedValue.text = done.toString()
                    textUncompletedValue.text = todo.toString()
                } else {
                    // If any response failed — show zeros
                    textCompletedValue.text = "0"
                    textUncompletedValue.text = "0"
                }

            } catch (e: Exception) {
                Log.e("ProfileActivity", "Ошибка загрузки отчета", e)
                // on error, show zeros so cards are not empty
                textCompletedValue.text = "0"
                textUncompletedValue.text = "0"
            }
        }
    }

    private fun updateBarChart(
        categoryData: Map<Int, Int>,
        categoryColors: Map<Int, Int>,
        categoryNames: Map<Int, String>
    ) {
        if (categoryData.isEmpty()) return

        // Преобразуем Map в список пар с индексом
        val entries = categoryData.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        // DataSet
        val dataSet = BarDataSet(entries, "Задачи по категориям").apply {
            colors = categoryData.keys.map { id -> categoryColors[id] ?: Color.GRAY }
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry?): String {
                    return barEntry?.y?.toInt().toString()
                }
            }
        }

        val barData = BarData(dataSet).apply { barWidth = 0.6f }

        barChart.apply {
            data = barData
            description.isEnabled = false
            setFitBars(true)
            setDrawGridBackground(false)
            setDrawValueAboveBar(true)
            animateY(800)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.WHITE
                textSize = 12f
                labelCount = categoryData.size
                valueFormatter = IndexAxisValueFormatter(
                    categoryData.keys.map { id -> if (id == 0) "Все" else categoryNames[id] ?: "?" }
                )
            }

            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                setLabelCount((categoryData.values.maxOrNull() ?: 0) + 1, true)
                setDrawGridLines(true)
                textColor = Color.WHITE
            }

            axisRight.apply {
                axisMinimum = 0f
                granularity = 1f
                setLabelCount((categoryData.values.maxOrNull() ?: 0) + 1, true)
                setDrawGridLines(false)
                textColor = Color.WHITE
            }

            legend.apply {
                textColor = Color.WHITE
                textSize = 14f
                form = Legend.LegendForm.SQUARE
            }

            invalidate()
        }
    }

}
