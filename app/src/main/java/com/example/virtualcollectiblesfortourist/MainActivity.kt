package com.example.virtualcollectiblesfortourist

import android.annotation.SuppressLint
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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView

import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_main)

        // Initialize map
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Initialize client for GPS location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request location permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            getCurrentLocation()
        }

        loadPlacesFromJson()

        // Initialize the drawer layout for navigation drawer functionality
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)

        // Set up the menu icon inside the search_bar.xml to toggle the drawer
        val menuIcon: ImageView = findViewById(R.id.menu_icon)
        menuIcon.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }

        // Set up navigation item selection listener for the NavigationView
        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_badges -> {
                    // Handle the badges action
                }
                R.id.nav_plan_trip -> {
                    // Handle the plan my trip action
                }
                R.id.nav_settings -> {
                    // Handle the settings action
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    // Getting current location
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLocation = GeoPoint(location.latitude, location.longitude)
                map.controller.setZoom(18.0)
                map.controller.setCenter(currentLocation)

                val marker = Marker(map)
                marker.position = currentLocation

                // Resize and set current position marker
                val drawable = ContextCompat.getDrawable(this, R.drawable.current_location)
                if (drawable != null) {
                    val width = 16
                    val height = 16
                    val bitmap = Bitmap.createScaledBitmap(
                        (drawable as BitmapDrawable).bitmap,
                        width,
                        height,
                        false
                    )
                    marker.icon = BitmapDrawable(resources, bitmap)
                }

                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                // Disable default info window popup on click
                marker.infoWindow = null

                map.overlays.add(marker)
                map.invalidate()
            }
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
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
                val location = place.getString("place").split(",")[0]
                val rarity = place.getString("rarity")

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
                    "common" -> R.drawable.map_pin_bg_common_grey
                    "rare" -> R.drawable.map_pin_bg_rare
                    "epic" -> R.drawable.map_pin_bg_epic
                    else -> R.drawable.map_pin_bg_common_grey
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
                marker.setAnchor(0.17355f, Marker.ANCHOR_BOTTOM)
                marker.icon = BitmapDrawable(resources, bitmap)

                // Disable default info window popup on click
                marker.infoWindow = null

                // Set marker click listener to zoom in when clicked
                marker.setOnMarkerClickListener { clickedMarker, mapView ->
                    mapView.controller.setZoom(18.0)
                    val offsetFactor = 0.0005
                    val offsetPosition = GeoPoint(
                        clickedMarker.position.latitude,
                        clickedMarker.position.longitude + offsetFactor
                    )

                    mapView.controller.animateTo(offsetPosition)
                    true
                }

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