package com.example.trainschedule

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
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
                btnSelectDate.text = selectedSearchDate

                btnSelectDate.setBackgroundColor(android.graphics.Color.parseColor("#EDE7F6"))
                btnSelectDate.setTextColor(android.graphics.Color.parseColor("#5E35B1"))
                (btnSelectDate as com.google.android.material.button.MaterialButton).apply {
                    strokeWidth = 0
                    iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#5E35B1"))
                }
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_seat_selection, null)
        val spinnerWagon = dialogView.findViewById<Spinner>(R.id.spinnerWagon)
        val rvSeats = dialogView.findViewById<RecyclerView>(R.id.rvSeats)
        val btnBuyTicket = dialogView.findViewById<Button>(R.id.btnBuyTicket)

        val carriageCount = train.carriageCount ?: 10
        val wagons = (1..carriageCount).map { "Вагон №$it" }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, wagons)
        spinnerWagon.adapter = spinnerAdapter

        rvSeats.layoutManager = GridLayoutManager(this, 4)
        var currentSelectedSeat: Seat? = null

        val seatAdapter = SeatAdapter(emptyList()) { seat ->
            currentSelectedSeat = seat
            if (seat != null) {
                btnBuyTicket.isEnabled = true
                btnBuyTicket.text = "ОПЛАТИТИ ${train.price} ₴"
            } else {
                btnBuyTicket.isEnabled = false
                btnBuyTicket.text = "ОПЛАТИТИ"
            }
        }
        rvSeats.adapter = seatAdapter

        val currentBookedSeats = train.bookedSeats ?: ""

        spinnerWagon.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val currentWagonNumber = position + 1

                val generatedSeats = (1..36).map { seatNum ->
                    val seatKey = "$currentWagonNumber-$seatNum"
                    val status = if (currentBookedSeats.contains(seatKey)) SeatStatus.OCCUPIED else SeatStatus.FREE
                    Seat(seatNum, status)
                }

                seatAdapter.updateSeats(generatedSeats)
                btnBuyTicket.isEnabled = false
                btnBuyTicket.text = "ОПЛАТИТИ"
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Рейс ${train.trainNumber}: ${train.startStation} → ${train.endStation}")
            .setView(dialogView)
            .setNegativeButton("Закрити", null)
            .show()

        btnBuyTicket.setOnClickListener {
            val selectedWagonNum = spinnerWagon.selectedItemPosition + 1
            val seatNum = currentSelectedSeat?.number
            val newSeatKey = "$selectedWagonNum-$seatNum"

            val updatedBookedSeats = if (currentBookedSeats.isEmpty()) {
                newSeatKey
            } else {
                "$currentBookedSeats,$newSeatKey"
            }

            val updatedTrain = train.copy(bookedSeats = updatedBookedSeats)

            btnBuyTicket.isEnabled = false
            btnBuyTicket.text = "ОБРОБКА..."

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val resultTrain = RetrofitClient.apiService.updateTrain(train.id!!, updatedTrain)

                    withContext(Dispatchers.Main) {
                        val index = allTrains.indexOfFirst { it.id == train.id }
                        if (index != -1) {
                            val mutableList = allTrains.toMutableList()
                            mutableList[index] = resultTrain
                            allTrains = mutableList

                            val dep = etDeparture.text.toString().trim()
                            val arr = etArrival.text.toString().trim()

                            if (dep.isNotEmpty() && arr.isNotEmpty() && selectedSearchDate.isNotEmpty() && !isAdminLoggedIn) {
                                val filteredList = allTrains.filter { t ->
                                    t.startStation.equals(dep, ignoreCase = true) &&
                                            t.endStation.equals(arr, ignoreCase = true) &&
                                            t.departureDate == selectedSearchDate
                                }
                                trainAdapter.updateData(filteredList)
                            } else {
                                trainAdapter.updateData(allTrains)
                            }
                        }

                        Toast.makeText(this@MainActivity, "✅ Оплата успішна!\nВагон $selectedWagonNum, Місце $seatNum", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Помилка зв'язку з сервером!", Toast.LENGTH_SHORT).show()
                        btnBuyTicket.isEnabled = true
                        btnBuyTicket.text = "ОПЛАТИТИ ${train.price} ₴"
                    }
                }
            }
        }
    }

    private fun showAdminLoginDialog() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val loginInput = EditText(this).apply {
            hint = "Логін"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val passwordInput = EditText(this).apply {
            hint = "Пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(loginInput)
        layout.addView(passwordInput)

        androidx.appcompat.app.AlertDialog.Builder(this)
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
                    Toast.makeText(this, "Режим адміна: керування маршрутами активовано", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Помилка: Невірний логін або пароль", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteRoute(train: TrainRoute) {
        androidx.appcompat.app.AlertDialog.Builder(this)
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
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val numInput = EditText(this).apply { hint = "Номер потяга" }
        val fromInput = EditText(this).apply { hint = "Звідки" }
        val toInput = EditText(this).apply { hint = "Куди" }
        val timeInput = EditText(this).apply { hint = "Час (напр. 14:00)" }

        val dateInput = EditText(this).apply {
            hint = "Оберіть дату"
            isFocusable = false
            isClickable = true
        }

        var finalDate = ""

        dateInput.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, day ->
                finalDate = String.format("%02d.%02d.%04d", day, month + 1, year)
                dateInput.setText(finalDate)
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        layout.addView(numInput)
        layout.addView(fromInput)
        layout.addView(toInput)
        layout.addView(timeInput)
        layout.addView(dateInput)

        androidx.appcompat.app.AlertDialog.Builder(this)
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
                    status = "OpenForBooking",
                    bookedSeats = ""
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
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val numInput = EditText(this).apply { setText(train.trainNumber) }
        val fromInput = EditText(this).apply { setText(train.startStation) }
        val toInput = EditText(this).apply { setText(train.endStation) }
        val timeInput = EditText(this).apply { setText(train.departureTime) }

        val dateInput = EditText(this).apply {
            setText(train.departureDate)
            isFocusable = false
            isClickable = true
        }

        var finalDate = train.departureDate

        dateInput.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val (day, month, year) = parseDate(finalDate ?: "")
            android.app.DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                finalDate = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear)
                dateInput.setText(finalDate)
            }, year, month, day).show()
        }

        val priceInput = EditText(this).apply { setText(train.price.toString()) }

        layout.addView(numInput)
        layout.addView(fromInput)
        layout.addView(toInput)
        layout.addView(timeInput)
        layout.addView(dateInput)
        layout.addView(priceInput)

        androidx.appcompat.app.AlertDialog.Builder(this)
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