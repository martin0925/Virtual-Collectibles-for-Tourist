package com.example.virtualcollectiblesfortourist.data

import androidx.room.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE id = 0 LIMIT 1")
    fun getDefaultUser(): User?

    @Update
    fun updateUser(user: User)
}