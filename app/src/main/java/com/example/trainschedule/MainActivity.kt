package com.example.trainschedule

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var trainAdapter: TrainAdapter

    private lateinit var etDeparture: EditText
    private lateinit var etArrival: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnAdminLogin: Button
    private lateinit var fabAddRoute: View
    private lateinit var btnSwap: ImageButton
    private lateinit var btnSortPrice: Button
    private lateinit var btnSortTime: Button
    private lateinit var btnSelectDate: Button

    private var allTrains: List<TrainRoute> = emptyList()

    private var isAdminLoggedIn = false

    private var isPriceAsc = true
    private var isTimeAsc = true

    private var selectedSearchDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        etDeparture = findViewById(R.id.etDeparture)
        etArrival = findViewById(R.id.etArrival)
        btnSearch = findViewById(R.id.btnSearch)
        btnAdminLogin = findViewById(R.id.btnAdminLogin)
        fabAddRoute = findViewById(R.id.fabAddRoute)
        btnSwap = findViewById(R.id.btnSwap)
        btnSortPrice = findViewById(R.id.btnSortPrice)
        btnSortTime = findViewById(R.id.btnSortTime)
        btnSelectDate = findViewById(R.id.btnSelectDate)

        recyclerView.layoutManager = LinearLayoutManager(this)

        trainAdapter = TrainAdapter(emptyList(), false,
            { selectedTrain -> showTrainDetailsDialog(selectedTrain) },
            { trainToDelete -> deleteRoute(trainToDelete) },
            { trainToEdit -> showEditRouteDialog(trainToEdit) }
        )
        recyclerView.adapter = trainAdapter

        btnSelectDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            android.app.DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedSearchDate = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear)
                btnSelectDate.text = "Дата: $selectedSearchDate"
            }, year, month, day).show()
        }

        btnSearch.setOnClickListener {
            val dep = etDeparture.text.toString().trim()
            val arr = etArrival.text.toString().trim()

            if (dep.isEmpty() || arr.isEmpty() || selectedSearchDate.isEmpty()) {
                Toast.makeText(this, "Введіть станції та оберіть дату!", Toast.LENGTH_SHORT).show()
            } else {
                searchTrains(dep, arr, selectedSearchDate)
            }
        }

        btnAdminLogin.setOnClickListener {
            if (isAdminLoggedIn) {
                isAdminLoggedIn = false
                btnAdminLogin.text = "ВХІД ДЛЯ ПЕРСОНАЛУ"
                btnAdminLogin.setTextColor(android.graphics.Color.parseColor("#8E8E93"))
                etDeparture.text.clear()
                etArrival.text.clear()
                btnSelectDate.text = "ОБРАТИ ДАТУ"
                selectedSearchDate = ""
                trainAdapter.updateData(emptyList())
                fabAddRoute.visibility = View.GONE
                trainAdapter.isStaffMode = false
                trainAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Вихід виконано. Активовано режим пасажира.", Toast.LENGTH_SHORT).show()
            } else {
                showAdminLoginDialog()
            }
        }

        fabAddRoute.setOnClickListener {
            showAddRouteDialog()
        }

        btnSwap.setOnClickListener {
            val from = etDeparture.text.toString()
            val to = etArrival.text.toString()
            etDeparture.setText(to)
            etArrival.setText(from)
        }

        btnSortPrice.setOnClickListener {
            val currentList = trainAdapter.trains
            if (currentList.isEmpty()) return@setOnClickListener

            val sortedList = if (isPriceAsc) {
                currentList.sortedBy { it.price }
            } else {
                currentList.sortedByDescending { it.price }
            }

            trainAdapter.updateData(sortedList)
            isPriceAsc = !isPriceAsc
            Toast.makeText(this, if (isPriceAsc) "Від найдешевшого" else "Від найдорожчого", Toast.LENGTH_SHORT).show()
        }

        btnSortTime.setOnClickListener {
            val currentList = trainAdapter.trains
            if (currentList.isEmpty()) return@setOnClickListener

            val sortedList = if (isTimeAsc) {
                currentList.sortedBy { it.departureTime }
            } else {
                currentList.sortedByDescending { it.departureTime }
            }

            trainAdapter.updateData(sortedList)
            isTimeAsc = !isTimeAsc
            Toast.makeText(this, if (isTimeAsc) "Спочатку ранні" else "Спочатку пізні", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchTrains(departure: String, arrival: String, date: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allTrains = RetrofitClient.apiService.getTrains()
                this@MainActivity.allTrains = allTrains

                val filteredList = allTrains.filter { train ->
                    train.startStation.equals(departure, ignoreCase = true) &&
                            train.endStation.equals(arrival, ignoreCase = true) &&
                            train.departureDate == date
                }

                withContext(Dispatchers.Main) {
                    if (filteredList.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Потягів на $date не знайдено", Toast.LENGTH_LONG).show()
                        trainAdapter.updateData(emptyList())
                    } else {
                        trainAdapter.updateData(filteredList)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("TrainSearch", "Помилка завантаження: ${e.message}")
                    Toast.makeText(this@MainActivity, "Помилка зв'язку з базою даних", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showTrainDetailsDialog(train: TrainRoute) {
        val message = """
            Маршрут: ${train.startStation} → ${train.endStation}
            Час відправлення: ${train.departureTime}
            Дата: ${train.departureDate}

            Тип потяга: ${train.trainType}
            Кількість вагонів: ${train.carriageCount}

            Вартість квитка: ${train.price} грн
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
            .setTitle("Деталі рейсу №${train.trainNumber}")
            .setMessage(message)
            .setPositiveButton("Купити квиток") { dialog, _ ->
                Toast.makeText(this@MainActivity, "Перехід до оплати...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Закрити") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showAdminLoginDialog() {
        val layout = android.widget.LinearLayout(this@MainActivity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val loginInput = EditText(this@MainActivity).apply {
            hint = "Логін"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val passwordInput = EditText(this@MainActivity).apply {
            hint = "Пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(loginInput)
        layout.addView(passwordInput)

        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
            .setTitle("Авторизація персоналу")
            .setView(layout)
            .setPositiveButton("Увійти") { dialog, _ ->
                if (loginInput.text.toString() == "admin" && passwordInput.text.toString() == "admin") {
                    isAdminLoggedIn = true
                    btnAdminLogin.text = "ВИЙТИ З РЕЖИМУ АДМІНА"
                    btnAdminLogin.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                    etDeparture.text.clear()
                    etArrival.text.clear()
                    btnSelectDate.text = "ОБРАТИ ДАТУ"
                    selectedSearchDate = ""
                    trainAdapter.updateData(allTrains)
                    trainAdapter.isStaffMode = true
                    trainAdapter.notifyDataSetChanged()
                    fabAddRoute.visibility = View.VISIBLE
                    Toast.makeText(this@MainActivity, "Режим адміна: керування маршрутами активовано", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Помилка: Невірний логін або пароль", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteRoute(train: TrainRoute) {
        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
            .setTitle("Видалення маршруту")
            .setMessage("Ви впевнені, що хочете видалити рейс №${train.trainNumber}?")
            .setPositiveButton("Видалити") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RetrofitClient.apiService.deleteTrain(train.id!!)

                        withContext(Dispatchers.Main) {
                            allTrains = allTrains.filter { it.id != train.id }
                            trainAdapter.updateData(allTrains)

                            if (etDeparture.text.isNotEmpty() && etArrival.text.isNotEmpty()) {
                                val dep = etDeparture.text.toString().trim()
                                val arr = etArrival.text.toString().trim()
                                val filteredList = allTrains.filter { t ->
                                    t.startStation.equals(dep, ignoreCase = true) &&
                                            t.endStation.equals(arr, ignoreCase = true)
                                }
                                trainAdapter.updateData(filteredList)
                            }

                            Toast.makeText(this@MainActivity, "Рейс назавжди видалено з БД", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Помилка сервера при видаленні", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun showAddRouteDialog() {
        val layout = android.widget.LinearLayout(this@MainActivity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val numInput = EditText(this@MainActivity).apply { hint = "Номер потяга" }
        val fromInput = EditText(this@MainActivity).apply { hint = "Звідки" }
        val toInput = EditText(this@MainActivity).apply { hint = "Куди" }
        val timeInput = EditText(this@MainActivity).apply { hint = "Час (напр. 14:00)" }

        val dateInput = EditText(this@MainActivity).apply {
            hint = "Оберіть дату"
            isFocusable = false
            isClickable = true
        }

        var finalDate = ""

        dateInput.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this@MainActivity, { _, year, month, day ->
                finalDate = String.format("%02d.%02d.%04d", day, month + 1, year)
                dateInput.setText(finalDate)
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        layout.addView(numInput)
        layout.addView(fromInput)
        layout.addView(toInput)
        layout.addView(timeInput)
        layout.addView(dateInput)

        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
            .setTitle("Додати новий маршрут")
            .setView(layout)
            .setPositiveButton("Зберегти") { dialog, _ ->
                val newRoute = TrainRoute(
                    tripId = (10..999).random(),
                    trainNumber = numInput.text.toString(),
                    startStation = fromInput.text.toString(),
                    endStation = toInput.text.toString(),
                    departureDate = finalDate,
                    departureTime = timeInput.text.toString(),
                    price = 500.0,
                    trainType = "Пасажирський",
                    carriageCount = 10,
                    status = "OpenForBooking"
                )

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val createdTrain = RetrofitClient.apiService.addTrain(newRoute)

                        withContext(Dispatchers.Main) {
                            val updatedList = allTrains.toMutableList()
                            updatedList.add(createdTrain)
                            allTrains = updatedList
                            trainAdapter.updateData(allTrains)
                            Toast.makeText(this@MainActivity, "Рейс успішно збережено на сервері!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Помилка збереження на сервер", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showEditRouteDialog(train: TrainRoute) {
        val layout = android.widget.LinearLayout(this@MainActivity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val numInput = EditText(this@MainActivity).apply { setText(train.trainNumber) }
        val fromInput = EditText(this@MainActivity).apply { setText(train.startStation) }
        val toInput = EditText(this@MainActivity).apply { setText(train.endStation) }
        val timeInput = EditText(this@MainActivity).apply { setText(train.departureTime) }

        val dateInput = EditText(this@MainActivity).apply {
            setText(train.departureDate)
            isFocusable = false
            isClickable = true
        }

        var finalDate = train.departureDate

        dateInput.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val (day, month, year) = parseDate(finalDate ?: "")
            android.app.DatePickerDialog(this@MainActivity, { _, selectedYear, selectedMonth, selectedDay ->
                finalDate = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear)
                dateInput.setText(finalDate)
            }, year, month, day).show()
        }

        val priceInput = EditText(this@MainActivity).apply { setText(train.price.toString()) }

        layout.addView(numInput)
        layout.addView(fromInput)
        layout.addView(toInput)
        layout.addView(timeInput)
        layout.addView(dateInput)
        layout.addView(priceInput)

        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
            .setTitle("Редагувати маршрут")
            .setView(layout)
            .setPositiveButton("Оновити") { dialog, _ ->
                val updatedRoute = train.copy(
                    trainNumber = numInput.text.toString(),
                    startStation = fromInput.text.toString(),
                    endStation = toInput.text.toString(),
                    departureTime = timeInput.text.toString(),
                    departureDate = finalDate,
                    price = priceInput.text.toString().toDoubleOrNull() ?: train.price
                )

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val resultTrain = RetrofitClient.apiService.updateTrain(train.id!!, updatedRoute)

                        withContext(Dispatchers.Main) {
                            val index = allTrains.indexOfFirst { it.id == train.id }
                            if (index != -1) {
                                val mutableList = allTrains.toMutableList()
                                mutableList[index] = resultTrain
                                allTrains = mutableList
                                trainAdapter.updateData(allTrains)
                                Toast.makeText(this@MainActivity, "Рейс успішно оновлено!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Помилка оновлення на сервері", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun parseDate(dateString: String): Triple<Int, Int, Int> {
        val parts = dateString.split(".")
        val day = parts[0].toInt()
        val month = parts[1].toInt() - 1
        val year = parts[2].toInt()
        return Triple(day, month, year)
    }
}