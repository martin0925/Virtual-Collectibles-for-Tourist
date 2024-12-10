package com.example.virtualcollectiblesfortourist

import android.content.Intent
import android.os.Bundle
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

        val sharedPreferences = getSharedPreferences("TripPrefs", MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPreferences.getString("savedPlaces", null)

        savedObjects = if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<MutableList<Place>>() {}.type
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        displaySavedTrip()
    }


    private fun displaySavedTrip() {
        val tripListView: ListView = findViewById(R.id.trip_list_view)
        val deleteTripButton: Button = findViewById(R.id.delete_trip_button)

        tripListView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            savedObjects.map { it.title }
        )

        deleteTripButton.setOnClickListener {
            savedObjects.clear() // Vymaže uložený seznam

            // Vymažeme SharedPreferences
            val sharedPreferences = getSharedPreferences("TripPrefs", MODE_PRIVATE)
            sharedPreferences.edit()
                .remove("savedPlaces") // Odstraní uložený seznam
                .putBoolean("hasExistingTrip", false) // Nastaví hasExistingTrip na false
                .apply()

            Toast.makeText(this, "Trip deleted", Toast.LENGTH_SHORT).show()

            // Přesměrujeme uživatele zpět na MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Zavře ViewTripActivity
        }

    }
}