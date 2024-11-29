package com.example.virtualcollectiblesfortourist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places WHERE rarity = :rarity")
    fun getPlacesByRarity(rarity: String): List<Place>

    @Query("SELECT * FROM places")
    fun getAllPlaces(): List<Place>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlaces(places: List<Place>)
}
