package com.example.virtualcollectiblesfortourist

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.virtualcollectiblesfortourist.data.AppDatabase
import com.example.virtualcollectiblesfortourist.data.Place
import kotlinx.coroutines.*

class PlanTripActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var currentObjects: MutableList<Place> = mutableListOf()
    private var savedObjects: MutableList<Place> = mutableListOf()
    private var currentIndex = 0
    private var selectedDistance: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.plan_trip_activity)

        database = AppDatabase.getDatabase(this)

        showDistanceSelectionPopup()

        setupButtons()
    }

    private fun showDistanceSelectionPopup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.distance_selection_popup, null)
        val distanceSeekBar: SeekBar = dialogView.findViewById(R.id.seekbar_distance)
        val distanceTextView: TextView = dialogView.findViewById(R.id.textview_distance)

        distanceSeekBar.progress = 0
        distanceTextView.text = "None"

        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                selectedDistance = progress
                distanceTextView.text = if (progress == 0) "None" else "$progress km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogTitle)
            .setTitle("Select Max Distance")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                loadAllObjectsFromDatabase()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#FFFFFF")))
        dialog.show()
    }

    private fun loadAllObjectsFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val allPlaces = database.placeDao().getAllPlaces()

            withContext(Dispatchers.Main) {
                currentObjects.clear()
                currentObjects.addAll(allPlaces)
                currentIndex = 0
                if (currentObjects.isNotEmpty()) {
                    displayCurrentObject()
                } else {
                    Toast.makeText(this@PlanTripActivity, "No objects found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayCurrentObject() {
        if (currentIndex < currentObjects.size) {
            val currentPlace = currentObjects[currentIndex]
            val imageView: ImageView = findViewById(R.id.place_image)
            val titleView: TextView = findViewById(R.id.place_title)

            Glide.with(this).load(currentPlace.imageUrl).into(imageView)
            titleView.text = currentPlace.title
        } else {
            currentIndex = 0
            displayCurrentObject()
        }
    }

    private fun setupButtons() {
        val likeButton: Button = findViewById(R.id.like_button)
        val dislikeButton: Button = findViewById(R.id.dislike_button)
        val showSavedPlacesButton: Button = findViewById(R.id.show_saved_places_button)

        likeButton.setOnClickListener {
            saveCurrentObject()
            showNextObject()
        }

        dislikeButton.setOnClickListener {
            showNextObject()
        }

        showSavedPlacesButton.setOnClickListener {
            showSavedPlacesScreen()
        }
    }

    private fun saveCurrentObject() {
        if (currentIndex < currentObjects.size) {
            val currentPlace = currentObjects[currentIndex]
            if (!savedObjects.contains(currentPlace)) {
                savedObjects.add(currentPlace)
            }
        }
    }

    private fun showNextObject() {
        currentIndex++
        if (currentIndex >= currentObjects.size) {
            currentIndex = 0
        }
        displayCurrentObject()
    }

    private fun showSavedPlacesScreen() {
        val intent = Intent(this, SavedPlacesActivity::class.java)
        intent.putExtra("savedPlaces", ArrayList(savedObjects))
        startActivity(intent)
    }
}
