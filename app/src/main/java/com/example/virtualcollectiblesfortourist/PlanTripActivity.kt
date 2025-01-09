package com.example.virtualcollectiblesfortourist

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.virtualcollectiblesfortourist.data.AppDatabase
import com.example.virtualcollectiblesfortourist.data.Place
import kotlinx.coroutines.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class PlanTripActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var currentObjects: MutableList<Place> = mutableListOf()
    private var savedObjects: MutableList<Place> = mutableListOf()
    private var currentIndex = 0
    private var selectedDistance: Int = 0
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.plan_trip_activity)

        setupStatusBar()

        userLatitude = intent.getDoubleExtra("latitude", 0.0)
        userLongitude = intent.getDoubleExtra("longitude", 0.0)

        database = AppDatabase.getDatabase(this)

        val sharedPreferences = getSharedPreferences("TripPrefs", MODE_PRIVATE)
        val hasExistingTrip = sharedPreferences.getBoolean("hasExistingTrip", false)

        if (hasExistingTrip) {
            setContentView(R.layout.view_trip_activity)
            showSavedTripScreen()
        } else {
            setContentView(R.layout.plan_trip_activity)
            showDistanceSelectionPopup()
        }
    }

    private fun showDistanceSelectionPopup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.distance_selection_popup, null)
        val distanceSeekBar: SeekBar = dialogView.findViewById(R.id.seekbar_distance)
        val distanceTextView: TextView = dialogView.findViewById(R.id.textview_distance)

        distanceSeekBar.progress = 10
        selectedDistance = 10
        distanceTextView.text = "$selectedDistance km"

        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                selectedDistance = progress
                distanceTextView.text = if (progress == 0) "None" else "$progress km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val alertDialog = AlertDialog.Builder(this, R.style.DistanceDialogTheme)
            .setTitle("Select Max Distance")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                loadAvailablePlaces()
            }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.show()

        // Nastavit barvu pisma pro tlacitka natvrdo, jinak byla vzdy barva nepochopitelne bila
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

    private fun loadAvailablePlaces() {
        CoroutineScope(Dispatchers.IO).launch {
            val allPlaces = database.placeDao().getAllPlaces()

            val filteredPlaces = allPlaces.filter { place ->
                val (placeLat, placeLon) = parseCoordinates(place.coordinates)
                val distance = calculateDistance(userLatitude, userLongitude, placeLat, placeLon)
                distance <= selectedDistance
            }

            withContext(Dispatchers.Main) {
                currentObjects.clear()
                currentObjects.addAll(filteredPlaces)
                currentIndex = 0
                if (currentObjects.isNotEmpty()) {
                    showPlaceSelectionScreen()
                } else {
                    Toast.makeText(this@PlanTripActivity, "No places found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parseCoordinates(coordinates: String): Pair<Double, Double> {
        val coords = coordinates.split(",").map { it.trim().toDouble() }
        return Pair(coords[0], coords[1])
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun showPlaceSelectionScreen() {
        val likeButton: Button = findViewById(R.id.like_button)
        val dislikeButton: Button = findViewById(R.id.dislike_button)
        val finishButton: Button = findViewById(R.id.finish_button)

        likeButton.setOnClickListener {
            saveCurrentPlace()  // Uloží místo do seznamu
            showNextPlace()  // Zobrazí další místo
        }

        dislikeButton.setOnClickListener {
            showNextPlace()  // Zobrazí další místo bez přidání do seznamu
        }

        finishButton.setOnClickListener {
            if (savedObjects.isNotEmpty()) { // Pokud máme alespoň jedno místo vybrané
                val sharedPreferences = getSharedPreferences("TripPrefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                // Uložení seznamu jako JSON
                val gson = com.google.gson.Gson()
                val json = gson.toJson(savedObjects)
                editor.putString("savedPlaces", json)
                editor.putBoolean("hasExistingTrip", true)
                editor.apply()

                val intent = Intent(this, ViewTripActivity::class.java)
                startActivity(intent)
                finish() // Zavře tuto aktivitu (PlanTripActivity)
            } else {
                Toast.makeText(this, "Please select at least one place", Toast.LENGTH_SHORT).show()
            }
        }

        displayCurrentPlace()  // Zobrazí aktuální místo
    }

    private fun setupStatusBar() {
        window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = Color.TRANSPARENT
        }
    }

    private fun saveCurrentPlace() {
        // Zajistíme, že místo bude přidáno pouze, pokud ještě není v seznamu
        if (currentIndex < currentObjects.size) {
            val currentPlace = currentObjects[currentIndex]
            if (!savedObjects.contains(currentPlace)) {
                savedObjects.add(currentPlace)  // Přidáme místo do seznamu
            }
        }
    }

    private fun showNextPlace() {
        currentIndex++
        if (currentIndex >= currentObjects.size) {
            currentIndex = 0
        }
        displayCurrentPlace()
    }

    private fun displayCurrentPlace() {
        val imageView: ImageView = findViewById(R.id.place_image)
        val titleView: TextView = findViewById(R.id.place_title)

        if (currentObjects.isNotEmpty() && currentIndex < currentObjects.size) {
            val currentPlace = currentObjects[currentIndex]
            val lightGray = ContextCompat.getColor(this, R.color.light_gray)
            Glide.with(this)
                .load(currentPlace.imageUrl)
                .placeholder(ColorDrawable(lightGray))
                .into(imageView)

            titleView.text = currentPlace.title
        } else {
            titleView.text = "No places available"
        }
    }

    private fun showSavedTripScreen() {
        val tripListView: ListView = findViewById(R.id.trip_list_view)
        val deleteButton: Button = findViewById(R.id.delete_trip_button)

        // Nacte ulozene objekty
        val sharedPreferences = getSharedPreferences("TripPrefs", MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val savedPlacesJson = sharedPreferences.getString("savedPlaces", "[]")
        val savedPlacesType = object : com.google.gson.reflect.TypeToken<List<Place>>() {}.type
        savedObjects = gson.fromJson(savedPlacesJson, savedPlacesType)

        tripListView.adapter = ArrayAdapter(
            this,
            R.layout.list_item,
            savedObjects.map { it.title }
        )

        deleteButton.setOnClickListener {
            savedObjects.clear()

            val sharedPreferences = getSharedPreferences("TripPrefs", MODE_PRIVATE)
            sharedPreferences.edit().remove("savedPlaces").apply()
            sharedPreferences.edit().putBoolean("hasPlannedTrips", false).apply()

            Toast.makeText(this, "Trip deleted", Toast.LENGTH_SHORT).show()

            // Přejdeme na MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Zavře tuto aktivitu (PlanTripActivity)
        }
    }
}