package com.example.photoboothapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionDao {
    @Insert
     fun insertAll(vararg session: Session) // Insert a session into the database

    @Delete
     fun delete(session: Session)

    @Query("SELECT * FROM Session")
     fun getAll(): List<Session> // Get all sessions from the database

     @Query("SELECT MAX(uid) FROM Session")
     fun getMaxUid(): Int?

}