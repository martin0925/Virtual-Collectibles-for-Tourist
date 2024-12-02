package com.example.virtualcollectiblesfortourist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val country: String,
    val imageUrl: String,
    val email: String,
    val city: String,
    val phoneNumber: String
)