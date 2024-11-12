package com.example.virtualcollectiblesfortourist

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat

import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_main)

        // Initialize map
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // TODO: now starting on exact location, make on current GPS location
        val startPoint = GeoPoint(50.0870, 14.4208)
        map.controller.setZoom(18.0)
        map.controller.setCenter(startPoint)

        // Permission for exact location
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        loadPlacesFromJson()
    }

    private fun loadPlacesFromJson() {
        try {
            // TODO: using reduced version of dataset, including rarity
            val inputStream: InputStream = assets.open("czech_places_reduced.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val placesArray = JSONArray(jsonString)

            for (i in 0 until placesArray.length()) {
                val place = placesArray.getJSONObject(i)
                val name = place.getString("title")
                val coordinates = place.getString("coordinates")
                val location = place.getString("place")
                val rarity = place.getString("value")

                // Split the coordinates into latitude and longitude
                val coords = coordinates.split(",").map { it.trim().toDouble() }
                val lat = coords[0]
                val lon = coords[1]

                // Inflate custom layout for the pin
                val inflater = layoutInflater
                val pinView = inflater.inflate(R.layout.map_pin, null)

                // Set title and subtitle in the custom pin layout
                val titleView = pinView.findViewById<TextView>(R.id.pin_title)
                val subtitleView = pinView.findViewById<TextView>(R.id.pin_subtitle)
                titleView.text = name
                subtitleView.text = location

                // Select background based on location rarity
                val backgroundRes = when (rarity) {
                    "legendary" -> R.drawable.map_pin_bg_legendary
                    "common" -> R.drawable.map_pin_bg_common
                    "rare" -> R.drawable.map_pin_bg_rare
                    "epic" -> R.drawable.map_pin_bg_epic
                    else -> R.drawable.map_pin_bg_common
                }
                pinView.background = ContextCompat.getDrawable(this, backgroundRes)

                // Create a bitmap
                pinView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                pinView.layout(0, 0, pinView.measuredWidth, pinView.measuredHeight)
                val bitmap = Bitmap.createBitmap(pinView.measuredWidth, pinView.measuredHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                pinView.draw(canvas)

                // Create custom marker
                val marker = Marker(map)
                marker.position = GeoPoint(lat, lon)
                marker.title = name
                marker.snippet = location
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.icon = BitmapDrawable(resources, bitmap)

                // Disable default info window popup on click
                marker.infoWindow = null

                map.overlays.add(marker)
            }

            map.invalidate()
        } catch (e: Exception) {
            Log.e("Error", "Error loading markers: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}