// StudentEntity.kt
package com.shadowamitendu.simpleattendance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey
    val roll: String,
    val name: String,
    var isPresent: Boolean = false
)