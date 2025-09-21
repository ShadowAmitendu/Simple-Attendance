package com.shadowamitendu.simpleattendance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface StudentDao {

    @Insert
    suspend fun insertStudent(student: StudentEntity)

    @Insert
    suspend fun insertAll(students: List<StudentEntity>)

    @Query("SELECT * FROM students")
    suspend fun getAllStudents(): List<StudentEntity>

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("DELETE FROM students")
    suspend fun deleteAll()
}
