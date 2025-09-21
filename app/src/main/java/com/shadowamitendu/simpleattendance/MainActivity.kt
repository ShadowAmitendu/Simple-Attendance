package com.shadowamitendu.simpleattendance

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.shadowamitendu.simpleattendance.adapter.StudentAdapter
import com.shadowamitendu.simpleattendance.data.AppDatabase
import com.shadowamitendu.simpleattendance.data.StudentEntity
import com.shadowamitendu.simpleattendance.data.StudentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: StudentAdapter
    private lateinit var studentList: MutableList<StudentEntity>
    private lateinit var pdfExporter: PdfExporter
    private lateinit var repository: StudentRepository

    // Views
    private lateinit var rvStudents: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var tvStudentCount: TextView
    private lateinit var fabExport: ExtendedFloatingActionButton
    private lateinit var btnImport: MaterialButton
    private lateinit var emptyState: LinearLayout

    // Date management
    private var selectedDate: Date = Date()
    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private val dateFormatForFile = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    // CSV File Picker
    private val csvPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    importCSV(uri)
                }
            }
        }

    // PDF Save Picker
    private val pdfSaverLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val dateString = dateFormatForFile.format(selectedDate)
                        pdfExporter.exportAttendanceToUri(this, studentList, uri, dateString)
                        Toast.makeText(this, "PDF saved successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "PDF Export Failed: ${e.message}", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
        initViews()
        setupDatabase()
        setupRecyclerView()
        setupClickListeners()
        loadStudentsFromDatabase()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d("MENU_DEBUG", "onCreateOptionsMenu called")
        menuInflater.inflate(R.menu.menu, menu)
        Log.d("MENU_DEBUG", "Menu inflated, items count: ${menu?.size}")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("MENU_CLICK", "Menu item clicked: ${item.itemId}, Title: ${item.title}")
        return when (item.itemId) {
            R.id.action_add_student -> {
                Log.d("MENU_CLICK", "Add Student clicked")
                showAddStudentDialog()
                true
            }

            R.id.action_clear_data -> {
                Log.d("MENU_CLICK", "Clear Data clicked")
                showClearDataDialog()
                true
            }

            R.id.action_reset_attendance -> {
                Log.d("MENU_CLICK", "Reset Attendance clicked")
                showResetAttendanceDialog()
                true
            }

            else -> {
                Log.d("MENU_CLICK", "Unknown menu item: ${item.itemId}")
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun initViews() {
        // Set up toolbar first
        val toolbar =
            findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        rvStudents = findViewById(R.id.rvStudents)
        tvDate = findViewById(R.id.tvDate)
        tvStudentCount = findViewById(R.id.tvStudentCount)
        fabExport = findViewById(R.id.fabExport)
        btnImport = findViewById(R.id.btnImport)
        emptyState = findViewById(R.id.emptyState)

        // Format and display current date
        updateDateDisplay()

        // Initialize PDF Exporter
        pdfExporter = PdfExporter()
    }

    private fun updateDateDisplay() {
        tvDate.text = dateFormat.format(selectedDate)
    }

    private fun setupDatabase() {
        val db = AppDatabase.getDatabase(this)
        repository = StudentRepository(db.studentDao())
    }

    private fun setupRecyclerView() {
        studentList = mutableListOf()
        adapter = StudentAdapter(studentList) { student, _ ->
            CoroutineScope(Dispatchers.IO).launch {
                repository.updateStudent(student)
            }
        }
        rvStudents.layoutManager = LinearLayoutManager(this)
        rvStudents.adapter = adapter
    }

    private fun setupClickListeners() {
        // Date picker click listener
        tvDate.setOnClickListener {
            showDatePicker()
        }

        fabExport.setOnClickListener {
            if (::studentList.isInitialized && studentList.isNotEmpty()) {
                val dateString = dateFormatForFile.format(selectedDate)
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_TITLE, "Attendance_$dateString.pdf")
                }
                pdfSaverLauncher.launch(intent)
            } else {
                Toast.makeText(this, "No students to export", Toast.LENGTH_SHORT).show()
            }
        }

        btnImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf("text/csv", "text/comma-separated-values", "application/vnd.ms-excel")
                )
            }
            csvPicker.launch(intent)
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(selectedDate.time)
            .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate = Date(selection)
            updateDateDisplay()
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun showAddStudentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_student, null)
        val rollInput = dialogView.findViewById<TextInputEditText>(R.id.etRoll)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.etName)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Student")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val roll = rollInput.text.toString().trim()
                val name = nameInput.text.toString().trim()

                if (roll.isNotEmpty() && name.isNotEmpty()) {
                    addStudent(roll, name)
                } else {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addStudent(roll: String, name: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newStudent = StudentEntity(roll = roll, name = name)
                repository.insertStudent(newStudent)

                studentList.add(newStudent)

                runOnUiThread {
                    updateUI()
                    Toast.makeText(
                        this@MainActivity,
                        "Student added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to add student: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all students and their attendance records. This action cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.baseline_warning_24)
            .show()
    }

    private fun clearAllData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.deleteAll()
                studentList.clear()

                runOnUiThread {
                    updateUI()
                    Toast.makeText(
                        this@MainActivity,
                        "All data cleared successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to clear data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showResetAttendanceDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Attendance")
            .setMessage("This will reset attendance status for all students to 'not marked'. Student data will be preserved.")
            .setPositiveButton("Reset") { _, _ ->
                resetAttendance()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(R.drawable.baseline_refresh_24)
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun resetAttendance() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                studentList.forEach { student ->
                    student.isPresent = false // Reset to default absent state
                    repository.updateStudent(student)
                }

                runOnUiThread {
                    adapter.notifyDataSetChanged()
                    Toast.makeText(
                        this@MainActivity,
                        "Attendance reset successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to reset attendance: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadStudentsFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            studentList.clear()
            studentList.addAll(repository.getAllStudents())

            runOnUiThread {
                updateUI()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateUI() {
        adapter.notifyDataSetChanged()
        updateStudentCount()
        updateEmptyState()
    }

    private fun updateStudentCount() {
        tvStudentCount.text = studentList.size.toString()
    }

    private fun updateEmptyState() {
        if (studentList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvStudents.visibility = View.GONE
            fabExport.hide()
        } else {
            emptyState.visibility = View.GONE
            rvStudents.visibility = View.VISIBLE
            fabExport.show()
        }
    }

    // Runtime Permission Check
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // Handle different Android versions properly
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                }
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11-12 (API 30-32)
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }

            else -> {
                // Android 6-10 (API 23-29)
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        // Request runtime permissions if needed
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }

        // Handle MANAGE_EXTERNAL_STORAGE for Android 11+ (separate from runtime permissions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showManageStoragePermissionDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showManageStoragePermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Storage Access Required")
            .setMessage("This app needs access to manage all files to export PDFs. You'll be redirected to settings to grant this permission.")
            .setPositiveButton("Grant Permission") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = "package:$packageName".toUri()
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general manage all files settings
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(
                    this,
                    "Storage permission is required for PDF export",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionRationaleDialog(deniedPermissions: List<String>) {
        val message = when {
            deniedPermissions.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    deniedPermissions.contains(android.Manifest.permission.READ_MEDIA_IMAGES) ->
                "Storage permissions are required to import CSV files and export PDF reports."

            deniedPermissions.contains(android.Manifest.permission.POST_NOTIFICATIONS) ->
                "Notification permission helps you stay updated about export progress."

            else -> "These permissions are required for the app to function properly."
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    deniedPermissions.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(
                    this,
                    "Some features may not work without permissions",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Permissions Denied")
            .setMessage("You have denied permissions. To enable them, please go to Settings > Apps > Simple Attendance > Permissions and grant the required permissions.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isNotEmpty()) {
                val shouldShowRationale = deniedPermissions.any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
                }

                if (shouldShowRationale) {
                    showPermissionRationaleDialog(deniedPermissions)
                } else {
                    // User selected "Don't ask again"
                    showPermissionDeniedDialog()
                }
            } else {
                Toast.makeText(this, "Permissions granted successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Check MANAGE_EXTERNAL_STORAGE permission status when returning from settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission granted
                Log.d("Permissions", "MANAGE_EXTERNAL_STORAGE granted")
            } else {
                Log.d("Permissions", "MANAGE_EXTERNAL_STORAGE not granted")
            }
        }
    }

    // Read CSV and Insert into DB
    private fun importCSV(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))

                val importedStudents = mutableListOf<StudentEntity>()
                var lineNumber = 0

                reader.lineSequence().forEach { line ->
                    lineNumber++

                    // Skip empty lines and header (first line if it contains "roll" or "name")
                    if (line.trim().isEmpty() ||
                        (lineNumber == 1 && (line.lowercase().contains("roll") || line.lowercase()
                            .contains("name")))
                    ) {
                        return@forEach
                    }

                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val roll = parts[0].trim()
                        val name = parts[1].trim()

                        // Skip if roll or name is empty
                        if (roll.isNotEmpty() && name.isNotEmpty()) {
                            // Check for duplicates in the import list
                            if (importedStudents.none { it.roll == roll }) {
                                importedStudents.add(StudentEntity(roll = roll, name = name))
                            }
                        }
                    }
                }

                if (importedStudents.isNotEmpty()) {
                    // Clear existing data and insert new students
                    repository.deleteAll()
                    repository.insertAll(importedStudents)

                    studentList.clear()
                    studentList.addAll(repository.getAllStudents())

                    runOnUiThread {
                        updateUI()
                        Toast.makeText(
                            this@MainActivity,
                            "CSV Imported Successfully (${importedStudents.size} students)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "No valid student data found in CSV",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "CSV Import Failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}