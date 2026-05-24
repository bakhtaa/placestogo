package com.example.placesproject.data.dao

import androidx.room.*
import com.example.placesproject.data.model.Comment

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment)

    @Query("SELECT * FROM comments WHERE photoId = :photoId")
    suspend fun getCommentsForPhoto(photoId: String): List<Comment>
}