package com.example.virtualcollectiblesfortourist

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
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
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.Normalizer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), FilterPopup.FilterDialogListener {

    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var profileUpdateReceiver: BroadcastReceiver
    private lateinit var searchField: EditText

    private var selectedDistanceRange: Int = 0

    // A set of active filters representing the rarity of places (common, rare, epic, legendary)
    private val activeFilters = mutableSetOf(
        "common", "rare", "epic", "legendary"
    )

    // A map that categorizes places with related tags for filtering
    private val categoryTags = mapOf(
        "museums" to setOf(
            "museums", "collections", "museums and collections", "art museum", "history",
            "literature", "repositories of knowledge", "subterranean sites"
        ),
        "nature" to setOf(
            "nature", "gardens", "plants", "rock formations", "geological oddities",
            "natural wonders", "geology", "ecosystems", "lakes", "water"
        ),
        "historical" to setOf(
            "architecture", "architectural oddities", "historical", "palaces", "castles",
            "sacred spaces", "urban planning", "monuments", "medieval"
        ),
        "art" to setOf(
            "art", "sculptures", "statues", "david cerny", "installations", "literature",
            "pop culture", "music history", "photography"
        ),
        "unusual" to setOf(
            "ossuaries", "alchemy", "magic", "subterranean sites", "mechanical instruments",
            "automata", "riddles", "strange science", "eccentric homes", "giant heads", "haunted",
            "witchcraft", "legends"
        )
    ).toMutableMap()

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupStatusBar()
        setupOsmdroidConfiguration()
        setupMap()
        setupSideMenu()
        setupSearchField()
        setupFilterButton()
        setupLocationClient()
        setupCurrentLocationButton()

        val sharedPreferences = getSharedPreferences("TripPrefs", MODE_PRIVATE)
        val gson = com.google.gson.Gson()
        val json = sharedPreferences.getString("savedPlaces", null)

        // If there are no saved places, set the trip flag to false
        if (json == null || json.isEmpty()) {
            sharedPreferences.edit().putBoolean("hasExistingTrip", false).apply()
        }

        // Initialize the database and load places from JSON or database
        val database = AppDatabase.getDatabase(this)
        val placeDao = database.placeDao()
        loadPlacesFromJsonToDb(this, database)
        loadPlacesFromDb()

        // Receiver to handle profile updates
        profileUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.virtualcollectiblesfortourist.PROFILE_UPDATED") {
                    val updatedName = intent.getStringExtra("name")
                    updateSideMenuProfileName(updatedName) // Update profile name in the side menu
                }
            }
        }
        registerReceiver(
            profileUpdateReceiver,
            IntentFilter("com.example.virtualcollectiblesfortourist.PROFILE_UPDATED"),
            RECEIVER_NOT_EXPORTED
        )

        // Search input field listener to show suggestions based on user input
        val searchField: AutoCompleteTextView = findViewById(R.id.search_field)
        val searchIcon: ImageView = findViewById(R.id.search_icon)

        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            // Listen to text changes in the search field and suggest places based on input
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val suggestions = placeDao.searchPlacesByTitle(query)
                        withContext(Dispatchers.Main) {
                            val adapter = ArrayAdapter(
                                this@MainActivity,
                                android.R.layout.simple_dropdown_item_1line,
                                suggestions
                            )
                            searchField.setAdapter(adapter)
                        }
                    }
                }
            }
        })

        // On selecting a suggestion, perform search for the selected place
        searchField.setOnItemClickListener { _, _, position, _ ->
            val selectedPlace = searchField.adapter.getItem(position) as String
            searchLocation(selectedPlace)
        }

        // Search button click listener to start search based on input in the search field
        searchIcon.setOnClickListener {
            val query = searchField.text.toString()
            if (query.isNotEmpty()) {
                searchLocation(query)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(profileUpdateReceiver) // Unregister the profile update receiver when the activity is destroyed
    }

    private fun updateSideMenuProfileName(name: String?) {
        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        val navHeader = navigationView.getHeaderView(0)
        if (navHeader != null) {
            val profileName = navHeader.findViewById<TextView>(R.id.profile_name)
            profileName.text = name
                ?: "Default Name" // Update the profile name in the side menu, use default if name is null
        }
    }

    private fun setupStatusBar() {
        window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = Color.TRANSPARENT // Make the status bar transparent
        }
    }

    private fun setupOsmdroidConfiguration() {
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        ) // Load osmdroid configuration
    }

    private fun setupMap() {
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK) // Set the tile source for the map
        map.setMultiTouchControls(true) // Enable multi-touch controls on the map

        map.addMapListener(object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                return false // Disable scrolling events on the map
            }

            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                if (event != null) {
                    val zoomLevel = map.zoomLevelDouble
                    updateMarkerVisibility(zoomLevel) // Update marker visibility based on zoom level
                }
                return true
            }
        })
    }

    private fun updateMarkerVisibility(zoomLevel: Double) {
        val shouldShowLargeMarkers = zoomLevel >= 14 // Show large markers if zoom level is >= 14
        map.overlays.clear() // Clear current overlays on the map

        val selectedTypeTags = activeFilters.flatMap { category ->
            categoryTags[category] ?: emptySet()
        }.toSet()

        // Logic to update marker visibility based on zoom level and filters
        if (shouldShowLargeMarkers) {
            userLocationMarker?.let { userMarker ->
                val userLocation = userMarker.position

                customMarkers.forEach { marker ->
                    val markerTags =
                        marker.subDescription?.split(", ")?.map { it.trim().lowercase() }
                            ?: emptyList()
                    if (marker != userLocationMarker) {
                        val markerData = marker.relatedObject as? MarkerData
                        val markerRarity = markerData?.rarity?.lowercase()

                        val matchesType = markerTags.any { it in selectedTypeTags }
                        val matchesRarity =
                            markerRarity != null && activeFilters.contains(markerRarity)

                        val distanceToMarker = userLocation.distanceToAsDouble(marker.position)
                        val withinDistance =
                            selectedDistanceRange == 0 || distanceToMarker <= selectedDistanceRange * 1000

                        if ((matchesType || selectedTypeTags.isEmpty()) &&
                            (matchesRarity || activeFilters.isEmpty()) &&
                            withinDistance
                        ) {
                            map.overlays.add(marker) // Add the marker to the map
                        }
                    }
                }
            }
        } else {
            userLocationMarker?.let { userMarker ->
                val userLocation = userMarker.position

                customSmallMarkers.forEach { marker ->
                    val markerTags =
                        marker.subDescription?.split(", ")?.map { it.trim().lowercase() }
                            ?: emptyList()
                    if (marker != userLocationMarker) {
                        val markerData = marker.relatedObject as? MarkerData
                        val markerRarity = markerData?.rarity?.lowercase()

                        val matchesType = markerTags.any { it in selectedTypeTags }
                        val matchesRarity =
                            markerRarity != null && activeFilters.contains(markerRarity)

                        val distanceToMarker = userLocation.distanceToAsDouble(marker.position)
                        val withinDistance =
                            selectedDistanceRange == 0 || distanceToMarker <= selectedDistanceRange * 1000

                        if ((matchesType || selectedTypeTags.isEmpty()) &&
                            (matchesRarity || activeFilters.isEmpty()) &&
                            withinDistance
                        ) {
                            map.overlays.add(marker) // Add the small marker to the map
                        }
                    }
                }
            }
        }
        userLocationMarker?.let { map.overlays.add(it) } // Add the user location marker to the map
        map.invalidate() // Redraw the map
    }

    private fun setupSideMenu() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        findViewById<ImageView>(R.id.menu_icon).setOnClickListener {
            // Open or close the side menu when the menu icon is clicked
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }

        val navigationView: NavigationView = findViewById(R.id.navigation_view)

        val navHeader = navigationView.getHeaderView(0)
        if (navHeader != null) {
            val profileImage = navHeader.findViewById<ImageView>(R.id.profile_image)
            val profileName = navHeader.findViewById<TextView>(R.id.profile_name)

            val userDao = AppDatabase.getDatabase(this).userDao()
            CoroutineScope(Dispatchers.IO).launch {
                val user = userDao.getDefaultUser()

                withContext(Dispatchers.Main) {
                    if (user != null) {
                        profileName.text = user.name // Set the user's name in the side menu
                        Glide.with(this@MainActivity)
                            .load(user.imageUrl) // Load user's profile image
                            .placeholder(R.drawable.profile_placeholder)
                            .circleCrop()
                            .into(profileImage)
                    }
                }
            }

            // Navigate to the ProfileActivity when profile image or name is clicked
            profileImage.setOnClickListener {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }

            profileName.setOnClickListener {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
        }

        // Set up side menu item listeners
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_badges -> {
                    val intent = Intent(this, BadgeActivity::class.java)
                    startActivity(intent)
                }

                R.id.nav_plan_trip -> {
                    userLocationMarker?.let { marker ->
                        val intent = Intent(this, PlanTripActivity::class.java).apply {
                            putExtra("latitude", marker.position.latitude)
                            putExtra("longitude", marker.position.longitude)
                        }
                        startActivity(intent)
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.END) // Close the side menu after selecting an item
            true
        }
    }

    private fun setupFilterButton() {
        // Set up the filter button click listener to show the filter dialog
        findViewById<ImageView>(R.id.filter_button).setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        // Show the filter dialog with the current distance range and active filters
        val filterDialog = FilterPopup(selectedDistanceRange)
        filterDialog.setActiveFilters(activeFilters)
        filterDialog.show(
            supportFragmentManager,
            "com.example.virtualcollectiblesfortourist.FilterPopup"
        )
    }

    private fun setupLocationClient() {
        // Initialize the location client and request permissions if necessary
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            // If permission is granted, start location updates
            startLocationUpdates()
        }
    }

    private var isFirstLocationUpdate = true
    private var userLocationMarker: Marker? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        // Create a location request for updates at specified intervals
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // When location results are available, update the user location marker
                locationResult.lastLocation?.let { location ->
                    updateUserLocationMarker(location.latitude, location.longitude)
                }
            }
        }
        // Start location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    private fun updateUserLocationMarker(latitude: Double, longitude: Double) {
        // Update the user location marker with the current coordinates
        val currentLocation = GeoPoint(latitude, longitude)

        if (userLocationMarker == null) {
            // Create the user location marker if it doesn't exist
            userLocationMarker = Marker(map).apply {
                position = currentLocation
                icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.current_location)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setOnMarkerClickListener { _, _ -> true }
                map.overlays.add(this)
            }
        } else {
            // Update the position of the existing user location marker
            userLocationMarker?.position = currentLocation
        }

        if (isFirstLocationUpdate) {
            // Center the map and set zoom level on the first location update
            map.controller.setZoom(18.0)
            map.controller.setCenter(currentLocation)
            isFirstLocationUpdate = false
        }

        map.invalidate()
    }

    private fun updateMapWithLocation(latitude: Double, longitude: Double) {
        // Update the map with the provided coordinates
        val currentLocation = GeoPoint(latitude, longitude)
        map.controller.setZoom(18.0)
        map.controller.setCenter(currentLocation)

        // Create a marker for the current location
        val marker = Marker(map).apply {
            position = currentLocation
            icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.current_location)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            setOnMarkerClickListener { _, _ -> true }
        }

        // Clear existing overlays and add the new marker
        map.overlays.clear()
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun setStaticLocation() {
        // Set a static location (Brno coordinates) on the map
        updateMapWithLocation(49.1928, 16.6090) // Static Brno location
    }

    private fun setupCurrentLocationButton() {
        // Set up the current location button to center the map on the user's current location
        val currentLocationButton = findViewById<ImageView>(R.id.btn_current_location)
        currentLocationButton.setOnClickListener {
            // Check if location permission is granted
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // If granted, get the last known location and center the map on it
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
                // Log if location permission is not granted
                Log.e("Permission", "Location permission not granted")
            }
        }
    }

    private fun showPopup(marker: Marker) {
        // Create and show a popup dialog with information about the marker
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
            val margin =
                (resources.displayMetrics.widthPixels * 0.05).toInt()  // Set margin as 5% of screen width
            layoutParams.width =
                resources.displayMetrics.widthPixels - 2 * margin  // Adjust dialog width
            layoutParams.height =
                WindowManager.LayoutParams.WRAP_CONTENT  // Set height to wrap content
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
        locationView.text =
            "${marker.snippet} ($coordinatesText)"  // Display location with coordinates

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
        rarityView.background = ContextCompat.getDrawable(
            this,
            R.drawable.badge_rarity
        )  // Set background based on rarity

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
        val tags =
            marker.subDescription?.split(", ")?.map { it.replace("\"", "").trim() } ?: emptyList()
        for (tag in tags) {
            val tagView = TextView(this)
            tagView.text = tag
            tagView.setPadding(16, 8, 16, 8)  // Add padding to the tags
            tagView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            tagView.background =
                ContextCompat.getDrawable(this, R.drawable.tag_background)  // Background for tags
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
        if (placeId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val placeDao = AppDatabase.getDatabase(applicationContext).placeDao()
                val place = placeDao.getPlaceById(placeId)

                withContext(Dispatchers.Main) {
                    if (place.collected) {
                        // Deactivate the button and change text if the place is already collected
                        collectButton.isEnabled = false
                        collectButton.text = "Already collected"
                        collectButton.setBackgroundColor(
                            ContextCompat.getColor(
                                this@MainActivity,
                                android.R.color.darker_gray
                            )
                        )
                    } else {
                        // Set up the collect button for uncollected places
                        collectButton.isEnabled = true
                        collectButton.text = "Collect"
                        collectButton.setOnClickListener {
                            CoroutineScope(Dispatchers.IO).launch {
                                placeDao.updatePlace(placeId)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Place collected!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    collectButton.isEnabled = false
                                    collectButton.text = "Already collected"
                                    collectButton.setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@MainActivity,
                                            android.R.color.darker_gray
                                        )
                                    )
                                }
                            }
                            dialog.dismiss()
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun loadPlacesFromDb() {
        val database = AppDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            // Get all places from the database
            val places = database.placeDao().getAllPlaces()
            val batchSize = 50  // Define a batch size for processing the places in chunks
            places.chunked(batchSize).forEach { batch ->
                withContext(Dispatchers.Main) {
                    // For each batch, create markers for the places and update the map
                    for (place in batch) {
                        createMarkerFromPlace(place)
                    }
                    map.invalidate()  // Refresh the map to show the new markers
                }
            }
        }
    }

    private fun createMarkerFromPlace(place: Place) {
        // Parse the coordinates and create a GeoPoint for the marker
        val (lat, lon) = parseCoordinates(place.coordinates)
        val position = GeoPoint(lat, lon)

        // Create a custom pin for the place and add it to the map
        createCustomPinBitmap(
            id = place.id,
            name = place.title,
            location = place.place,
            rarity = place.rarity,
            imageUrl = place.imageUrl,
            position = position,
            description = place.tags,
            objectUrl = place.objectUrl,
            coordinates = place.coordinates,
        )
    }

    private fun parseCoordinates(coordinates: String): Pair<Double, Double> {
        // Split the coordinates string and parse them into latitude and longitude
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
        coordinates: String,
    ) {
        // Inflate the layout for the pin view
        val inflater = layoutInflater
        val pinView = inflater.inflate(R.layout.map_pin, null)

        val titleView = pinView.findViewById<TextView>(R.id.pin_title)
        val subtitleView = pinView.findViewById<TextView>(R.id.pin_subtitle)
        val imageView = pinView.findViewById<ImageView>(R.id.pin_image)

        titleView.text = name
        subtitleView.text = location

        // Set the background resource for the pin based on rarity
        val backgroundRes = when (rarity) {
            "legendary" -> R.drawable.map_pin_bg_legendary
            "common" -> R.drawable.map_pin_bg_common_grey
            "rare" -> R.drawable.map_pin_bg_rare
            "epic" -> R.drawable.map_pin_bg_epic
            else -> R.drawable.map_pin_bg_common_grey
        }
        pinView.background = ContextCompat.getDrawable(this, backgroundRes)

        // Load the image from the URL using Glide and create a bitmap for the pin
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
                    val bitmap = Bitmap.createBitmap(
                        pinView.measuredWidth,
                        pinView.measuredHeight,
                        Bitmap.Config.ARGB_8888
                    )
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
                    // Log error if image loading fails
                    Log.e("Debug", "Failed to load image for URL: $imageUrl")
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Set placeholder image if image load is cleared
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

    private val customMarkers = mutableListOf<Marker>()
    private val customSmallMarkers = mutableListOf<Marker>()

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
        // Create a marker for the place with custom properties
        val marker = Marker(map).apply {
            this.position = position
            this.title = title
            this.snippet = location
            this.setAnchor(0.17355f, Marker.ANCHOR_BOTTOM)
            this.icon = BitmapDrawable(resources, iconBitmap)
            this.subDescription = description
            this.relatedObject = MarkerData(id, objectUrl, rarity, coordinates, imageUrl)
        }

        // Create a smaller version of the marker icon for small markers
        val smallMarkerIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.map_pin_small)
            ?.let { drawable ->
                val originalBitmap = (drawable as BitmapDrawable).bitmap
                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 21, 26, true)
                BitmapDrawable(resources, scaledBitmap)
            }

        // Create a small marker with the same position and description
        val smallMarker = Marker(map).apply {
            this.position = position
            this.icon = smallMarkerIcon
            this.setAnchor(0.17355f, Marker.ANCHOR_BOTTOM)
            this.subDescription = description
            this.relatedObject = MarkerData(id, objectUrl, rarity, coordinates, imageUrl)
        }

        // Set click listeners for the large and small markers
        marker.setOnMarkerClickListener { clickedMarker, mapView ->
            handleMarkerClick(clickedMarker, mapView, false)
        }

        smallMarker.setOnMarkerClickListener { clickedMarker, mapView ->
            handleMarkerClick(clickedMarker, mapView, true)
        }

        // Add both markers to custom lists and to the map
        customMarkers.add(marker)
        customSmallMarkers.add(smallMarker)

        map.overlays.add(marker)
        return marker
    }

    private fun handleMarkerClick(marker: Marker, mapView: MapView, isSmall: Boolean): Boolean {
        val offsetFactor = 0.0005  // Define an offset factor to adjust the map position
        val offsetPosition = GeoPoint(
            marker.position.latitude,
            marker.position.longitude + offsetFactor  // Slightly adjust the longitude for better view
        )
        mapView.controller.animateTo(offsetPosition)

        // Re-add clicked marker to set z-index to the top layer, making sure it's above others
        mapView.overlays.remove(marker)
        mapView.overlays.add(marker)

        if (!isSmall) {
            showPopup(marker)  // Show popup for the clicked marker if it's not a small one
        }
        updateMarkerVisibility(18.0)  // Update marker visibility after the click

        return true
    }

    private fun setupSearchField() {
        searchField = findViewById(R.id.search_field)

        // Listen for search input and detect when the user clicks the search button on the keyboard
        searchField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchField.text.toString()
                if (query.isNotEmpty()) {
                    searchLocation(query)  // Perform search if input is not empty
                }
                true
            } else {
                false
            }
        }
    }

    private fun searchLocation(query: String) {
        val geocoder = Geocoder(this)
        try {
            val normalizedQuery =
                normalizeString(query)  // Normalize the query string for comparison

            // Try to find an exact match for custom markers
            val matchingMarker = customMarkers.find {
                normalizeString(it.title) == normalizedQuery
            }

            if (matchingMarker != null) {
                // Adjust position and zoom if a matching marker is found
                val offsetPosition = calculateOffset(matchingMarker.position, 0.0, 0.0005)
                map.overlays.remove(matchingMarker)
                map.overlays.add(matchingMarker)
                map.controller.animateTo(offsetPosition)
                map.controller.setZoom(19.0)
            } else {
                // If no marker is found, try to find the location using Geocoder
                val addresses = geocoder.getFromLocationName(query, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val geoPoint = GeoPoint(address.latitude, address.longitude)

                    val offsetGeoPoint = calculateOffset(geoPoint, 0.0, 0.0005)
                    map.controller.setCenter(offsetGeoPoint)  // Center the map on the location
                    map.controller.setZoom(19.0)  // Zoom in to the location
                } else {
                    // Show a toast if no results are found for the query
                    Toast.makeText(this, "No results found for \"$query\"", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } catch (e: Exception) {
            // Show a toast in case of an error during location search
            Toast.makeText(this, "Error finding location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun normalizeString(input: String): String {
        // Normalize the string by removing diacritics and converting to lowercase
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("-", " ")
            .replace("’", "'")
            .lowercase()
            .trim()
    }

    private fun calculateOffset(
        geoPoint: GeoPoint,
        latOffset: Double,
        lonOffset: Double
    ): GeoPoint {
        // Calculate a new GeoPoint with applied offsets for latitude and longitude
        val newLatitude = geoPoint.latitude + latOffset
        val newLongitude = geoPoint.longitude + lonOffset
        return GeoPoint(newLatitude, newLongitude)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()  // Start location updates if permission is granted
        } else {
            setStaticLocation()  // Set a static location (Brno) if permission is not granted
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        // Start location updates if permission is granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        // Stop location updates when the activity is paused
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onFiltersSelected(filters: List<String>, distance: Int) {
        // Update active filters and selected distance when filters are applied
        activeFilters.clear()
        activeFilters.addAll(filters)
        selectedDistanceRange = distance
        updateFilteredMarkers()
    }

    private fun updateFilteredMarkers() {
        map.overlays.clear()

        // Get the selected type tags and rarities from the active filters
        val selectedTypeTags = activeFilters.flatMap { category ->
            categoryTags[category] ?: emptySet()
        }.toSet()

        val selectedRarities =
            activeFilters.filter { it in listOf("common", "rare", "epic", "legendary") }.toSet()

        userLocationMarker?.let { userMarker ->
            val userLocation = userMarker.position  // Get the user's current location

            customMarkers.forEach { marker ->
                val markerData = marker.relatedObject as? MarkerData
                val markerTags =
                    marker.subDescription?.split(", ")?.map { it.trim().lowercase() } ?: emptyList()
                val markerRarity = markerData?.rarity?.lowercase()

                val matchesType = markerTags.any { it in selectedTypeTags }
                val matchesRarity =
                    selectedRarities.isEmpty() || (markerRarity != null && markerRarity in selectedRarities)

                // Calculate the distance to the marker
                val distanceToMarker = userLocation.distanceToAsDouble(marker.position)

                val withinDistance =
                    selectedDistanceRange == 0 || distanceToMarker <= selectedDistanceRange * 1000 // Convert km to meters

                // Add marker if it matches selected filters and is within the distance range
                if ((matchesType || selectedTypeTags.isEmpty()) && matchesRarity && withinDistance) {
                    map.overlays.add(marker)
                }
            }
        }

        // Add the user's location marker to the map
        userLocationMarker?.let { map.overlays.add(it) }
        map.invalidate()  // Refresh the map with the filtered markers
    }

    fun GeoPoint.distanceTo(other: GeoPoint): Double {
        val earthRadius = 6371.0  // Earth's radius in kilometers

        // Calculate the change in latitude and longitude
        val dLat = Math.toRadians(other.latitude - this.latitude)
        val dLon = Math.toRadians(other.longitude - this.longitude)

        // Haversine formula to calculate distance between two points
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(this.latitude)) * cos(Math.toRadians(other.latitude)) *
                sin(dLon / 2).pow(2)

        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))  // Return the distance in kilometers
    }
}