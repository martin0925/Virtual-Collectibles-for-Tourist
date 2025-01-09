package com.example.virtualcollectiblesfortourist

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.virtualcollectiblesfortourist.data.Place

class SavedPlacesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_places_activity)

        setupStatusBar()

        // Retrieve the passed "savedPlaces" list (which is Serializable)
        val savedPlaces = intent.getSerializableExtra("savedPlaces") as? ArrayList<Place> ?: ArrayList()

        val placesTextView: TextView = findViewById(R.id.saved_places_text)
        placesTextView.text = savedPlaces.joinToString("\n") { it.title }
    }

    private fun setupStatusBar() {
        window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = Color.TRANSPARENT
        }
    }
}



