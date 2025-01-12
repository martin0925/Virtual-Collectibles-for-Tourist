package com.example.virtualcollectiblesfortourist

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.virtualcollectiblesfortourist.data.AppDatabase
import com.example.virtualcollectiblesfortourist.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var editUserName: EditText
    private lateinit var editUserTitle: EditText
    private lateinit var editEmail: EditText
    private lateinit var editCountry: EditText
    private lateinit var editCity: EditText
    private lateinit var saveProfileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Set up transparent status bar
        setupStatusBar()

        profileImage = findViewById(R.id.profileImage)
        editUserName = findViewById(R.id.editUserName)
        editUserTitle = findViewById(R.id.editUserTitle)
        editEmail = findViewById(R.id.editEmail)
        editCountry = findViewById(R.id.editCountry)
        editCity = findViewById(R.id.editCity)
        saveProfileButton = findViewById(R.id.saveProfileButton)

        // Load existing user profile
        loadUserProfile()

        // Save the updated profile when the button is clicked
        saveProfileButton.setOnClickListener {
            saveUserProfile()
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

    private fun loadUserProfile() {
        val userDao = AppDatabase.getDatabase(this).userDao()
        CoroutineScope(Dispatchers.IO).launch {
            // Fetch the user's profile from the database
            val user = userDao.getDefaultUser()

            withContext(Dispatchers.Main) {
                if (user != null) {
                    // Display the user's information in the UI
                    displayUserProfile(user)
                } else {
                    Toast.makeText(this@ProfileActivity, "User not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayUserProfile(user: User) {
        // Populate the UI fields with user data
        editUserName.setText(user.name)
        editUserTitle.setText("Common Traveller")
        editEmail.setText(user.email)
        editCountry.setText(user.country)
        editCity.setText(user.city)

        // Load the user's profile image
        Glide.with(this)
            .load(user.imageUrl)
            .placeholder(R.drawable.profile_placeholder)
            .circleCrop()
            .into(profileImage)
    }

    private fun saveUserProfile() {
        // Create a new User object with updated profile information
        val updatedUser = User(
            id = 0,
            name = editUserName.text.toString(),
            country = editCountry.text.toString(),
            imageUrl = "https://example.com/default-user.jpg",
            email = editEmail.text.toString(),
            city = editCity.text.toString(),
            phoneNumber = "000-000-000"
        )

        val userDao = AppDatabase.getDatabase(this).userDao()
        CoroutineScope(Dispatchers.IO).launch {
            // Update the user profile in the database
            userDao.updateUser(updatedUser)

            // Send a broadcast indicating the profile was updated
            val intent = Intent("com.example.virtualcollectiblesfortourist.PROFILE_UPDATED")
            intent.putExtra("name", updatedUser.name)
            sendBroadcast(intent)

            withContext(Dispatchers.Main) {
                // Show a confirmation toast to the user
                Toast.makeText(this@ProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}