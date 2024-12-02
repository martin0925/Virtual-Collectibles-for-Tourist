package com.example.virtualcollectiblesfortourist

import android.os.Bundle
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

        val recyclerView = findViewById<RecyclerView>(R.id.badgeRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // Load collected badges from database
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(applicationContext)
            val collectedPlaces = database.placeDao().getCollectedPlaces()

            withContext(Dispatchers.Main) {
                val adapter = BadgeAdapter(collectedPlaces)
                recyclerView.adapter = adapter
            }
        }
    }
}