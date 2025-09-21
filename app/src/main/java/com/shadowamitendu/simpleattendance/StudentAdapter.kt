package com.shadowamitendu.simpleattendance.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.shadowamitendu.simpleattendance.R
import com.shadowamitendu.simpleattendance.data.StudentEntity

class StudentAdapter(
    private val students: List<StudentEntity>,
    private val onAttendanceMarked: (StudentEntity, Boolean) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSerial: TextView = itemView.findViewById(R.id.tvSerial)
        val tvRoll: TextView = itemView.findViewById(R.id.tvRoll)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val btnPresent: MaterialButton = itemView.findViewById(R.id.btnPresent)
        val btnAbsent: MaterialButton = itemView.findViewById(R.id.btnAbsent)
        val cardView: MaterialCardView = itemView as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_student, parent, false)
        return StudentViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        val context: Context = holder.itemView.context

        // Set student data
        holder.tvSerial.text = (position + 1).toString()
        holder.tvRoll.text = "Roll: ${student.roll}"
        holder.tvName.text = student.name

        // Function to update button states and card appearance
        fun updateButtonColors() {
            when (student.isPresent) {
                true -> {
                    // Present state - Green theme
                    holder.btnPresent.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_green_light)
                    holder.btnAbsent.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.darker_gray)
                    holder.cardView.strokeColor = ContextCompat.getColor(context, android.R.color.holo_green_light)
                    holder.cardView.strokeWidth = 2
                }
                false -> {
                    // Absent state - Red theme
                    holder.btnAbsent.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_red_light)
                    holder.btnPresent.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.darker_gray)
                    holder.cardView.strokeColor = ContextCompat.getColor(context, android.R.color.holo_red_light)
                    holder.cardView.strokeWidth = 2
                }
            }
        }

        // Initial UI update
        updateButtonColors()

        // Set click listeners
        holder.btnPresent.setOnClickListener {
            student.isPresent = true
            updateButtonColors()
            onAttendanceMarked(student, true)
        }

        holder.btnAbsent.setOnClickListener {
            student.isPresent = false
            updateButtonColors()
            onAttendanceMarked(student, false)
        }
    }

    override fun getItemCount() = students.size

    // Helper method to get attendance summary
    fun getAttendanceSummary(): Pair<Int, Int> {
        val present = students.count { it.isPresent }
        val absent = students.count { !it.isPresent }
        return Pair(present, absent)
    }
}