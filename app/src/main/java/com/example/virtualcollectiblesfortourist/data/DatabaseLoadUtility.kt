package com.example.virtualcollectiblesfortourist.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

object DatabaseUtils {

    fun loadPlacesFromJsonToDb(context: Context, database: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val datasets = listOf("czech_places.json")
                val places = mutableListOf<Place>()

                for (dataset in datasets) {
                    val inputStream = context.assets.open(dataset)
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val place = Place(
                            title = jsonObject.getString("title"),
                            place = jsonObject.getString("place"),
                            coordinates = jsonObject.getString("coordinates"),
                            rarity = jsonObject.getString("rarity"),
                            tags = jsonObject.getJSONArray("tags").join(", "),
                            imageUrl = jsonObject.getString("image"),
                            objectUrl = jsonObject.getString("url"),
                            collected = false,
                            dateOfVisit = null
                        )
                        places.add(place)
                    }
                }

                database.placeDao().insertPlaces(places)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}