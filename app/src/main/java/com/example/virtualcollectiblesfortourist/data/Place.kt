package com.example.virtualcollectiblesfortourist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val place: String,
    val coordinates: String,
    val rarity: String,
    val tags: String,
    val imageUrl: String,
    val objectUrl: String,
    var collected: Boolean = false,
    var dateOfVisit: String? = null
) : Serializable