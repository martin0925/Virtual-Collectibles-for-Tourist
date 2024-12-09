package com.example.virtualcollectiblesfortourist

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.virtualcollectiblesfortourist.data.Place

class SavedPlacesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_places_activity)

        // Retrieve the passed "savedPlaces" list (which is Serializable)
        val savedPlaces = intent.getSerializableExtra("savedPlaces") as? ArrayList<Place> ?: ArrayList()

        val placesTextView: TextView = findViewById(R.id.saved_places_text)
        placesTextView.text = savedPlaces.joinToString("\n") { it.title }
    }
}

