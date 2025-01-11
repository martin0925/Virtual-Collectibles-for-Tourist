package com.example.virtualcollectiblesfortourist

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.virtualcollectiblesfortourist.data.Place

class ViewTripActivity : AppCompatActivity() {

    private lateinit var savedObjects: MutableList<Place>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_trip_activity)

        setupStatusBar()

        // Retrieve the saved places from SharedPreferences
        val sharedPreferences = getSharedPreferences("TripPrefs", MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPreferences.getString("savedPlaces", null)

        savedObjects = if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<MutableList<Place>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf() // No saved places, initialize an empty list
        }

        displaySavedTrip()
    }

    private fun displaySavedTrip() {
        val tripListView: ListView = findViewById(R.id.trip_list_view)
        val deleteTripButton: Button = findViewById(R.id.delete_trip_button)

        // Set up the ListView to display the saved places
        tripListView.adapter = ArrayAdapter(
            this,
            R.layout.list_item,
            savedObjects.map { it.title } // Display only the titles of the saved places
        )

        deleteTripButton.setOnClickListener {
            savedObjects.clear() // Clears the saved list

            // Clears SharedPreferences
            val sharedPreferences = getSharedPreferences("TripPrefs", MODE_PRIVATE)
            sharedPreferences.edit()
                .remove("savedPlaces") // Removes the saved list from SharedPreferences
                .putBoolean("hasExistingTrip", false) // Sets hasExistingTrip to false
                .apply()

            Toast.makeText(this, "Trip deleted", Toast.LENGTH_SHORT).show()

            // Redirects the user back to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Closes ViewTripActivity
        }
    }

    private fun setupStatusBar() {
        window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = Color.TRANSPARENT // Makes the status bar transparent
        }
    }
}
