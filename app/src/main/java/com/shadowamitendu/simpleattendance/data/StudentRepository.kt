package com.shadowamitendu.simpleattendance.data

class StudentRepository(private val studentDao: StudentDao) {

    suspend fun getAllStudents(): List<StudentEntity> {
        return studentDao.getAllStudents()
    }

    suspend fun insertStudent(student: StudentEntity) {
        studentDao.insertStudent(student)
    }

    suspend fun insertAll(students: List<StudentEntity>) {
        studentDao.insertAll(students)
    }

    suspend fun updateStudent(student: StudentEntity) {
        studentDao.updateStudent(student)
    }

    suspend fun deleteAll() {
        studentDao.deleteAll()
    }
}