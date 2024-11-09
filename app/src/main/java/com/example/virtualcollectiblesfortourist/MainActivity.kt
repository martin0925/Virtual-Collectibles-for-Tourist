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
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_main)

        // Initialize map
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // TODO: now starting on exact location, make on current GPS location
        val startPoint = GeoPoint(49.19522, 16.60796)
        map.controller.setZoom(18.0)
        map.controller.setCenter(startPoint)

        // Permission for exact location
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        loadPlacesFromJson()
    }

    // Resizing map custom pins to ideal size when zooming
    private fun resizeDrawable(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun loadPlacesFromJson() {
        try {
            val inputStream: InputStream = assets.open("czech_places.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            val placesArray = JSONArray(jsonString)

            for (i in 0 until placesArray.length()) {
                val place = placesArray.getJSONObject(i)
                val name = place.getString("title")
                val coordinates = place.getString("coordinates")
                val location = place.getString("place")
                val url = place.getString("url")

                // Split the coordinates into latitude and longitude
                val coords = coordinates.split(",").map { it.trim().toDouble() }
                val lat = coords[0]
                val lon = coords[1]

                // Create custom marker
                val marker = Marker(map)
                marker.position = GeoPoint(lat, lon)
                marker.title = name
                marker.snippet = location
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                // TODO: custom pin marker, now only one icon (museum)
                val drawable = ContextCompat.getDrawable(this, R.drawable.landmark_solid)
                val resizedBitmap = resizeDrawable(drawable!!, 64, 64)
                marker.icon = BitmapDrawable(resources, resizedBitmap)

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