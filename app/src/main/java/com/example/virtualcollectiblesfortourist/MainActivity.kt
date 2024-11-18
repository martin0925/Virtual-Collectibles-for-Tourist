package com.example.virtualcollectiblesfortourist

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
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
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

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

    private fun showPopup(marker: Marker) {
        // Create a new dialog instance
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_dialog)  // Set the layout for the dialog
        dialog.setCancelable(true)  // Make the dialog cancelable by tapping outside

        // Extract the related data (object URL, rarity, and coordinates) from the marker
        val relatedData = marker.relatedObject as? Triple<String, String, String>
        val objectUrl = relatedData?.first
        val rarity = relatedData?.second
        val coordinatesText = relatedData?.third

        // Adjust the dialog window size and make it look better
        val window = dialog.window
        if (window != null) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))  // Make the background transparent
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)
            val margin = (resources.displayMetrics.widthPixels * 0.05).toInt()  // Set margin as 5% of screen width
            layoutParams.width = resources.displayMetrics.widthPixels - 2 * margin  // Adjust dialog width
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT  // Set height to wrap content
            window.attributes = layoutParams  // Apply the new layout parameters
        }

        // Bind views from the dialog layout
        val titleView = dialog.findViewById<TextView>(R.id.info_title)
        val locationView = dialog.findViewById<TextView>(R.id.info_location)
        val urlButton = dialog.findViewById<Button>(R.id.info_url)
        val imageView = dialog.findViewById<ImageView>(R.id.info_image)
        val rarityView = dialog.findViewById<TextView>(R.id.info_rarity)
        val collectButton = dialog.findViewById<Button>(R.id.info_collect_button)
        // val closeButton = dialog.findViewById<ImageView>(R.id.close_button)  // Close button (commented out)

        // Set the text values for the title and location
        titleView.text = marker.title
        locationView.text = "${marker.snippet} ($coordinatesText)"  // Display location with coordinates

        // Set the URL button text and configure it to open the URL when clicked
        urlButton.text = "Visit Website"
        urlButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(objectUrl))
            startActivity(intent)  // Open the URL in a browser
        }

        // Set the rarity text and background color
        rarityView.text = rarity
        rarityView.background = ContextCompat.getDrawable(this, R.drawable.badge_rarity)  // Set background based on rarity

        // Define a color for the rarity (based on the string value)
        val rarityColor = when (rarity?.lowercase()) {
            "legendary" -> R.color.legendary_color
            "epic" -> R.color.epic_color
            "rare" -> R.color.rare_color
            "common" -> R.color.common_color
            else -> android.R.color.black  // Default color if rarity is unknown
        }

        // Set the tint color and text color for the rarity view
        rarityView.backgroundTintList = ContextCompat.getColorStateList(this, rarityColor)
        rarityView.setTextColor(ContextCompat.getColor(this, android.R.color.white))

        // Set a sample image in the image view
        imageView.setImageResource(R.drawable.sample_image)

        // Set up the tags container and dynamically add tags to the dialog
        val tagsContainer = dialog.findViewById<FlexboxLayout>(R.id.info_tags_container)
        tagsContainer.removeAllViews()  // Clear any existing tags

        // Split the tags from the marker description, clean them, and add them to the container
        val tags = marker.subDescription?.split(", ")?.map { it.replace("\"", "").trim() } ?: emptyList()
        for (tag in tags) {
            val tagView = TextView(this)
            tagView.text = tag
            tagView.setPadding(16, 8, 16, 8)  // Add padding to the tags
            tagView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            tagView.background = ContextCompat.getDrawable(this, R.drawable.tag_background)  // Background for tags
            tagView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)  // Set text size for tags

            // Set the layout for the tag and add margins
            val layoutParams = FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(8, 8, 8, 8)
            tagView.layoutParams = layoutParams

            tagsContainer.addView(tagView)  // Add the tag view to the container
        }

        // Set the collect button click listener to dismiss the dialog
        collectButton.setOnClickListener {
            dialog.dismiss()
        }

        // Uncomment this to enable the close button (currently not in use)
        // closeButton.setOnClickListener {
        //     dialog.dismiss()
        // }

        // Show the dialog
        dialog.show()
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
                val description = place.getJSONArray("tags").join(", ").replace("[", "").replace("]", "")
                val objectUrl = place.getString("url")

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
                marker.subDescription = description
                marker.relatedObject = Triple(objectUrl, rarity, coordinates)


                // Set marker click listener to zoom in when clicked
                marker.setOnMarkerClickListener { clickedMarker, mapView ->
                    mapView.controller.setZoom(18.0)
                    val offsetFactor = 0.0005
                    val offsetPosition = GeoPoint(
                        clickedMarker.position.latitude,
                        clickedMarker.position.longitude + offsetFactor
                    )

                    mapView.controller.animateTo(offsetPosition)
                    showPopup(clickedMarker)
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