package com.example.virtualcollectiblesfortourist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places WHERE id = :placeId")
    fun getPlaceById(placeId: Int): Place

    @Query("SELECT * FROM places WHERE rarity = :rarity")
    fun getPlacesByRarity(rarity: String): List<Place>

    @Query("SELECT * FROM places")
    fun getAllPlaces(): List<Place>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlaces(places: List<Place>)

    // Update only the collected status to true and set dateOfVisit to the current date
    @Query("UPDATE places SET collected = 1, dateOfVisit = strftime('%Y-%m-%d', 'now') WHERE id = :placeId")
    fun updatePlace(placeId: Int)

    @Query("SELECT * FROM places WHERE collected = 1")
    fun getCollectedPlaces(): List<Place>
}
