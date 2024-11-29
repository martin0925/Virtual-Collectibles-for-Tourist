package com.example.virtualcollectiblesfortourist

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.virtualcollectiblesfortourist.data.AppDatabase
import com.example.virtualcollectiblesfortourist.data.DatabaseUtils.loadPlacesFromJsonToDb
import com.example.virtualcollectiblesfortourist.data.Place
import com.google.android.flexbox.FlexboxLayout
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupStatusBar()
        setupOsmdroidConfiguration()
        setupMap()
        setupSideMenu()
        setupFilterButton()
        setupLocationClient()
        setupCurrentLocationButton()

        val database = AppDatabase.getDatabase(this)
        loadPlacesFromJsonToDb(this, database)
        loadPlacesFromDb()
    }

    private fun setupStatusBar() {
        window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = Color.TRANSPARENT
        }
    }

    private fun setupOsmdroidConfiguration() {
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
    }

    private fun setupMap() {
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
    }

    private fun setupSideMenu() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        findViewById<ImageView>(R.id.menu_icon).setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }

        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_badges -> { /* Handle badges action */ }
                R.id.nav_plan_trip -> { /* Handle plan my trip action */ }
                R.id.nav_settings -> { /* Handle settings action */ }
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            true
        }
    }

    private fun setupFilterButton() {
        findViewById<ImageView>(R.id.filter_button).setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val filterDialog = FilterPopup()
        filterDialog.show(supportFragmentManager, "com.example.virtualcollectiblesfortourist.FilterPopup")
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            startLocationUpdates()
        }
    }

    private var isFirstLocationUpdate = true
    private var userLocationMarker: Marker? = null

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateUserLocationMarker(location.latitude, location.longitude)
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun updateUserLocationMarker(latitude: Double, longitude: Double) {
        val currentLocation = GeoPoint(latitude, longitude)

        if (userLocationMarker == null) {
            // Create the marker for the first time
            userLocationMarker = Marker(map).apply {
                position = currentLocation
                icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.current_location)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                map.overlays.add(this)
            }
        } else {
            userLocationMarker?.position = currentLocation
        }

        // Center the map only on the first location update
        if (isFirstLocationUpdate) {
            map.controller.setZoom(18.0)
            map.controller.setCenter(currentLocation)
            isFirstLocationUpdate = false
        }

        map.invalidate()
    }

    private fun updateMapWithLocation(latitude: Double, longitude: Double) {
        val currentLocation = GeoPoint(latitude, longitude)
        map.controller.setZoom(18.0)
        map.controller.setCenter(currentLocation)

        // Create and set up the marker without a popup
        val marker = Marker(map).apply {
            position = currentLocation
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.current_location)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setOnMarkerClickListener { _, _ -> true }
        }

        map.overlays.clear()
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun setStaticLocation() {
        updateMapWithLocation(49.1928, 16.6090) // Static Brno location
    }

    private fun setupCurrentLocationButton() {
        val currentLocationButton = findViewById<ImageView>(R.id.btn_current_location)
        currentLocationButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLocation = GeoPoint(location.latitude, location.longitude)
                        map.controller.animateTo(currentLocation)
                        map.controller.setZoom(18.0)
                    } else {
                        Log.e("Location", "Location is null")
                    }
                }
            } else {
                Log.e("Permission", "Location permission not granted")
            }
        }
    }

    private fun showPopup(marker: Marker) {
        // Create a new dialog instance
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_dialog)  // Set the layout for the dialog
        dialog.setCancelable(true)  // Make the dialog cancelable by tapping outside

        // Extract the related data (object URL, rarity, coordinates, image URL) from the marker
        val relatedData = marker.relatedObject as? MarkerData
        val objectUrl = relatedData?.objectUrl
        val rarity = relatedData?.rarity
        val coordinatesText = relatedData?.coordinates
        val imageUrl = relatedData?.imageUrl
        val placeId = relatedData?.id

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
        if (rarity != null) {
            rarityView.text = rarity.uppercase()
        }
        rarityView.background = ContextCompat.getDrawable(this, R.drawable.badge_rarity)  // Set background based on rarity

        // Define a color for the rarity (based on the string value)
        val rarityColor = when (rarity?.lowercase()) {
            "legendary" -> R.color.legendary
            "epic" -> R.color.epic
            "rare" -> R.color.rare
            "common" -> R.color.common
            else -> R.color.common
        }

        // Set the tint color and text color for the rarity view
        rarityView.backgroundTintList = ContextCompat.getColorStateList(this, rarityColor)
        rarityView.setTextColor(ContextCompat.getColor(this, android.R.color.white))

        // Load the image from the URL
        imageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.sample_image)
                .error(R.drawable.sample_image)
                .into(imageView)
        }

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

        // Handle the collect button click
        // TODO: remove toast messages, replace with own popups
        collectButton.setOnClickListener {
            placeId?.let { id ->
                CoroutineScope(Dispatchers.IO).launch {
                    val placeDao = AppDatabase.getDatabase(applicationContext).placeDao()
                    val place = placeDao.getPlaceById(id)

                    if (place.collected) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Already collected", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        placeDao.updatePlace(id)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Place collected!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadPlacesFromDb() {
        val database = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            val places = database.placeDao().getAllPlaces()
            runOnUiThread {
                for (place in places) {
                    createMarkerFromPlace(place)
                }
            }
        }
    }

    private fun createMarkerFromPlace(place: Place) {
        val (lat, lon) = parseCoordinates(place.coordinates)
        val position = GeoPoint(lat, lon)

        createCustomPinBitmap(
            id = place.id,
            name = place.title,
            location = place.place,
            rarity = place.rarity,
            imageUrl = place.imageUrl,
            position = position,
            description = place.tags,
            objectUrl = place.objectUrl,
            coordinates = place.coordinates
        )
    }

    private fun parseCoordinates(coordinates: String): Pair<Double, Double> {
        val coords = coordinates.split(",").map { it.trim().toDouble() }
        return Pair(coords[0], coords[1])
    }

    private fun createCustomPinBitmap(
        id: Int,
        name: String,
        location: String,
        rarity: String,
        imageUrl: String,
        position: GeoPoint,
        description: String,
        objectUrl: String,
        coordinates: String
    ) {
        val inflater = layoutInflater
        val pinView = inflater.inflate(R.layout.map_pin, null)

        val titleView = pinView.findViewById<TextView>(R.id.pin_title)
        val subtitleView = pinView.findViewById<TextView>(R.id.pin_subtitle)
        val imageView = pinView.findViewById<ImageView>(R.id.pin_image)

        titleView.text = name
        subtitleView.text = location

        val backgroundRes = when (rarity) {
            "legendary" -> R.drawable.map_pin_bg_legendary
            "common" -> R.drawable.map_pin_bg_common_grey
            "rare" -> R.drawable.map_pin_bg_rare
            "epic" -> R.drawable.map_pin_bg_epic
            else -> R.drawable.map_pin_bg_common_grey
        }
        pinView.background = ContextCompat.getDrawable(this, backgroundRes)

        // Load the actual image from the provided URL using Glide
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .placeholder(R.drawable.sample_image)
            .error(R.drawable.sample_image)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    imageView.setImageBitmap(resource)

                    // Render the view to a bitmap after the image has loaded
                    pinView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    pinView.layout(0, 0, pinView.measuredWidth, pinView.measuredHeight)
                    val bitmap = Bitmap.createBitmap(pinView.measuredWidth, pinView.measuredHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    pinView.draw(canvas)

                    // Set up the marker with the final bitmap
                    val marker = setupMarker(
                        id = id,
                        position = position,
                        title = name,
                        location = location,
                        iconBitmap = bitmap,
                        description = description,
                        objectUrl = objectUrl,
                        rarity = rarity,
                        imageUrl = imageUrl,
                        coordinates = coordinates
                    )
                    map.overlays.add(marker)
                    map.invalidate()  // Refresh the map to show the marker
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.e("Debug", "Failed to load image for URL: $imageUrl")
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    imageView.setImageDrawable(placeholder)
                }
            })
    }

    data class MarkerData(
        val id: Int,
        val objectUrl: String,
        val rarity: String,
        val coordinates: String,
        val imageUrl: String
    )

    private fun setupMarker(
        id: Int,
        position: GeoPoint,
        title: String,
        location: String,
        iconBitmap: Bitmap,
        description: String,
        objectUrl: String,
        imageUrl: String,
        rarity: String,
        coordinates: String
    ): Marker {
        val marker = Marker(map).apply {
            this.position = position
            this.title = title
            this.snippet = location
            this.setAnchor(0.17355f, Marker.ANCHOR_BOTTOM)
            this.icon = BitmapDrawable(resources, iconBitmap)
            this.subDescription = description
            this.relatedObject = MarkerData(id, objectUrl, rarity, coordinates, imageUrl)
        }

        marker.setOnMarkerClickListener { clickedMarker, mapView ->
            handleMarkerClick(clickedMarker, mapView)
        }

        return marker
    }

    private fun handleMarkerClick(marker: Marker, mapView: MapView): Boolean {
        mapView.controller.setZoom(18.0)
        val offsetFactor = 0.0005
        val offsetPosition = GeoPoint(
            marker.position.latitude,
            marker.position.longitude + offsetFactor
        )
        mapView.controller.animateTo(offsetPosition)

        // Re-add clicked marker to set z-index to the top layer
        mapView.overlays.remove(marker)
        mapView.overlays.add(marker)

        showPopup(marker)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            setStaticLocation()  // Brno location if permission is not approved
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}