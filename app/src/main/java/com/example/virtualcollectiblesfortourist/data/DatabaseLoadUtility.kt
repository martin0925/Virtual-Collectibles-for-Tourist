package com.example.virtualcollectiblesfortourist.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

object DatabaseUtils {

    fun loadPlacesFromJsonToDb(context: Context, database: AppDatabase) {
        val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Check if database is already initialized
        if (sharedPreferences.getBoolean("isDatabaseInitialized", false)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val datasets = listOf("czech_places.json","belgium_places.json","luxembourg_places.json")
            val places = mutableListOf<Place>()

            try {
                for (dataset in datasets) {
                    context.assets.open(dataset).use { inputStream ->
                        val jsonString = inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = JSONArray(jsonString)

                        for (i in 0 until jsonArray.length()) {
                            val jsonObject = jsonArray.getJSONObject(i)
                            val tagsArray = jsonObject.getJSONArray("tags")

                            val tags = StringBuilder()
                            for (j in 0 until tagsArray.length()) {
                                tags.append(tagsArray.getString(j))
                                if (j < tagsArray.length() - 1) tags.append(", ")
                            }

                            val place = Place(
                                title = jsonObject.getString("title"),
                                place = jsonObject.getString("place").split(",")[0],
                                coordinates = jsonObject.getString("coordinates"),
                                rarity = jsonObject.getString("rarity"),
                                tags = tags.toString(),
                                imageUrl = jsonObject.getString("image"),
                                objectUrl = jsonObject.getString("url"),
                                collected = false,
                                dateOfVisit = null
                            )
                            places.add(place)
                        }
                    }
                }

                if (places.isNotEmpty()) {
                    database.placeDao().insertPlaces(places)
                    sharedPreferences.edit().putBoolean("isDatabaseInitialized", true).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}