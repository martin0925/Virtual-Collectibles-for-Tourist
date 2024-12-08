package com.example.virtualcollectiblesfortourist

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.virtualcollectiblesfortourist.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BadgeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badges)

        setupStatusBar()

        val recyclerView = findViewById<RecyclerView>(R.id.badgeRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val totalBadgesText = findViewById<TextView>(R.id.collectedBadgesCount)

        // Load collected badges from database
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(applicationContext)
            val collectedPlaces = database.placeDao().getCollectedPlaces()

            withContext(Dispatchers.Main) {
                val adapter = BadgeAdapter(collectedPlaces)
                recyclerView.adapter = adapter

                // Aktualizuj počet badge
                totalBadgesText.text = "${collectedPlaces.size}"
            }
        }
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
