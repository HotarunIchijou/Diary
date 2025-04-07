package org.kaorun.diary.ui.fragments

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat.is24HourFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.database.FirebaseDatabase
import org.kaorun.diary.R
import org.kaorun.diary.data.TasksDatabase
import org.kaorun.diary.databinding.FragmentBottomSheetBinding
import org.kaorun.diary.receivers.NotificationReceiver
import org.kaorun.diary.viewmodel.TasksViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var tasksViewModel: TasksViewModel
    private var _binding: FragmentBottomSheetBinding? = null
    private val binding get() = _binding!!
    private var taskText: String = ""
    private var formattedTime: String? = null
    private var selectedDate: Date? = null

    private var existingTaskId: String? = null

    companion object {
        fun newInstance(id: String, title: String, isCompleted: Boolean, time: String?): BottomSheetDialogFragment {
            val fragment = BottomSheetFragment()
            val args = Bundle().apply {
                putString("TASK_ID", id)
                putString("NOTE_TITLE", title)
                putBoolean("IS_COMPLETED", isCompleted)
                putString("TIME", time)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBottomSheetBinding.inflate(inflater, container, false)
        tasksViewModel = ViewModelProvider(requireActivity())[TasksViewModel::class.java]

        binding.buttonSave.setOnClickListener {
            taskText = binding.editText.text.toString().trim()

            if (taskText.isNotEmpty()) {
                val finalTime = formatTime(selectedDate, formattedTime)

                val task = TasksDatabase(
                    id = existingTaskId ?: FirebaseDatabase.getInstance().reference.push().key.orEmpty(),
                    title = taskText,
                    isCompleted = false,
                    time = finalTime
                )

                if (existingTaskId != null) {
                    tasksViewModel.updateTask(task)
                } else {
                    tasksViewModel.addTask(task)
                }

                if (formattedTime != null) scheduleNotification(selectedDate, formattedTime!!)
                else dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a task", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dateChip.isVisible = false
        binding.editText.requestFocus()

        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.editText, InputMethodManager.SHOW_IMPLICIT)

        setupTimeChip()
        setupDateChip()

        arguments?.let { args ->
            existingTaskId = args.getString("TASK_ID")
            val taskTitle = args.getString("NOTE_TITLE")
            val taskTime = args.getString("TIME")

            binding.editText.setText(taskTitle ?: "")

            if (!taskTime.isNullOrEmpty()) {
                formattedTime = taskTime.substringAfter(", ", taskTime)
                binding.timeChip.text = formattedTime
                binding.timeChip.isCloseIconVisible = true
                binding.dateChip.isVisible = true

                val dateString = taskTime.substringBefore(",", "")
                val parsedDate = tryParseDate(dateString)
                if (parsedDate != null) {
                    selectedDate = parsedDate
                    binding.dateChip.text = formatDateForChip(parsedDate)
                    binding.dateChip.isCloseIconVisible = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTimeChip() {
        val chip = binding.timeChip

        chip.setOnClickListener {
            showTimePickerDialog()
        }

        chip.setOnCloseIconClickListener {
            chip.text = getString(R.string.time)
            chip.isCloseIconVisible = false
            binding.dateChip.isVisible = false
            formattedTime = null
        }
    }

    private fun setupDateChip() {
        val chip = binding.dateChip

        chip.setOnClickListener {
            showDatePickerDialog()
        }

        chip.setOnCloseIconClickListener {
            chip.text = getString(R.string.date)
            chip.isCloseIconVisible = false
            selectedDate = null
        }
    }

    private fun showTimePickerDialog() {
        val isSystem24Hour = is24HourFormat(requireContext())
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val now = LocalTime.now()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(now.hour)
            .setMinute(now.minute)
            .setTitleText(getString(R.string.select_time))
            .setInputMode(INPUT_MODE_CLOCK)
            .build()

        picker.addOnPositiveButtonClickListener {
            val selectedHour = picker.hour
            val selectedMinute = picker.minute
            formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            binding.timeChip.text = formattedTime
            binding.timeChip.isCloseIconVisible = true
            binding.dateChip.isVisible = true
        }

        picker.show(parentFragmentManager, "time")
    }

    private fun showDatePickerDialog() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_date))
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            selectedDate = Date(selection)
            binding.dateChip.text = formatDateForChip(selectedDate)
            binding.dateChip.isCloseIconVisible = true
        }

        picker.show(parentFragmentManager, "date")
    }

    private fun formatTime(selectedDate: Date?, selectedTime: String?): String? {
        if (selectedTime == null) return null
        if (selectedDate == null) return selectedTime

        val localDate = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        val label = when (localDate) {
            today -> getString(R.string.date)
            today.minusDays(1) -> getString(R.string.date_yesterday)
            today.plusDays(1) -> getString(R.string.date_tomorrow)
            else -> localDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
        }

        return "$label, $selectedTime"
    }

    private fun formatDateForChip(selectedDate: Date?): String {
        val date = selectedDate ?: Date()
        val localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return when (localDate) {
            today -> getString(R.string.date)
            today.plusDays(1) -> getString(R.string.date_tomorrow)
            else -> {
                val pattern = if (localDate.year == today.year) "MMM d" else "MMM d, yyyy"
                localDate.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            }
        }
    }

    private fun tryParseDate(dateStr: String?): Date? {
        if (dateStr == null) return null
        val formats = listOf("MMM d", "MMM d, yyyy")
        for (format in formats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(format, Locale.getDefault())
                val localDate = LocalDate.parse(dateStr, formatter)
                return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun scheduleNotification(selectedDate: Date?, selectedTime: String) {
        if (selectedTime.isBlank()) return

        val timeParts = selectedTime.split(":")
        if (timeParts.size != 2) return

        val selectedHour = timeParts[0].toIntOrNull() ?: return
        val selectedMinute = timeParts[1].toIntOrNull() ?: return

        val date = selectedDate ?: Date()

        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), getString(R.string.must_be_in_the_future), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent().apply {
                setClassName(requireContext().packageName, "org.kaorun.diary.receivers.NotificationReceiver")
                putExtra("notification_title", taskText)
            }


            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )

            dismiss()
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), getString(R.string.grant_reminders_permission), Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }
}
