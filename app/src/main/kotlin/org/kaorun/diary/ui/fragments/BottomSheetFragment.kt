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
import org.kaorun.diary.utils.NotificationUtils.cancelNotification
import org.kaorun.diary.viewmodel.TasksViewModel
import java.time.LocalDate
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
    private var formattedDate: String? = null
    private var selectedDate: Date? = null
    private var isCompleted: Boolean = false
    private var existingTaskId: String? = null

    companion object {
        fun newInstance(
            id: String, title: String, isCompleted: Boolean,
            time: String?, date: String?
        ): BottomSheetFragment {
            val fragment = BottomSheetFragment()
            fragment.arguments = Bundle().apply {
                putString("TASK_ID", id)
                putString("TASK_TITLE", title)
                putBoolean("IS_COMPLETED", isCompleted)
                putString("TIME", time)
                putString("DATE", date)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomSheetBinding.inflate(inflater, container, false)
        tasksViewModel = ViewModelProvider(requireActivity())[TasksViewModel::class.java]

        binding.buttonSave.setOnClickListener {
            taskText = binding.editText.text.toString().trim()
            if (taskText.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.enter_task), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val taskId = existingTaskId ?: FirebaseDatabase.getInstance().reference.push().key.orEmpty()
            if (formattedTime != null && formattedDate == null) {
                val today = LocalDate.now()
                formattedDate = today.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
            }

            val task = TasksDatabase(taskId, taskText, isCompleted, formattedTime, formattedDate)

            if (formattedTime != null && !scheduleNotification(selectedDate, formattedTime!!, taskId)) {
                return@setOnClickListener
            }
            if (existingTaskId != null && formattedTime == null) {
                cancelNotification(requireContext(), taskId)
            }

            saveTask(task)
            dismiss()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.dateChip.isVisible = false
        binding.editText.requestFocus()
        setupTimeChip()
        setupDateChip()

        arguments?.let { args ->
            existingTaskId = args.getString("TASK_ID")
            isCompleted = args.getBoolean("IS_COMPLETED", false)
            binding.editText.setText(args.getString("TASK_TITLE", ""))

            args.getString("TIME")?.let {
                formattedTime = it
                binding.timeChip.text = it
                binding.timeChip.isCloseIconVisible = true
                binding.dateChip.isVisible = true
            }

            args.getString("DATE")?.let { receivedDate ->
                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)
                val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                val todayFormatted = today.format(formatter)
                val tomorrowFormatted = tomorrow.format(formatter)

                when (receivedDate) {
                    todayFormatted -> {
                        binding.dateChip.text = getString(R.string.date)
                        binding.dateChip.isCloseIconVisible = false
                        formattedDate = null
                    }
                    tomorrowFormatted -> {
                        binding.dateChip.text = getString(R.string.date_tomorrow)
                        binding.dateChip.isCloseIconVisible = true
                        formattedDate = tomorrowFormatted
                    }
                    else -> {
                        binding.dateChip.text = receivedDate
                        binding.dateChip.isCloseIconVisible = true
                        formattedDate = receivedDate
                    }
                }

                binding.dateChip.isVisible = true
            }

            binding.buttonDelete.visibility = View.VISIBLE
            binding.buttonDelete.setOnClickListener {
                existingTaskId?.let { id ->
                    tasksViewModel.deleteTask(requireContext(), id)
                    cancelNotification(requireContext(), id)
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTimeChip() {
        binding.timeChip.setOnClickListener { showTimePickerDialog() }
        binding.timeChip.setOnCloseIconClickListener {
            binding.timeChip.text = getString(R.string.time)
            binding.timeChip.isCloseIconVisible = false
            binding.dateChip.isVisible = false
            formattedTime = null
        }
    }

    private fun setupDateChip() {
        binding.dateChip.setOnClickListener { showDatePickerDialog() }
        binding.dateChip.setOnCloseIconClickListener {
            binding.dateChip.text = getString(R.string.date)
            binding.dateChip.isCloseIconVisible = false
            formattedDate = null
        }
    }

    private fun showTimePickerDialog() {
        val clockFormat = if (is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val now = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setInputMode(INPUT_MODE_CLOCK)
            .setTimeFormat(clockFormat)
            .setHour((now.get(Calendar.HOUR_OF_DAY) + 1) % 24)
            .setMinute(if (now.get(Calendar.MINUTE) > 30) 30 else 0)
            .setTitleText(getString(R.string.select_time))
            .build()

        picker.addOnPositiveButtonClickListener {
            formattedTime = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
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

        picker.addOnPositiveButtonClickListener { sel ->
            selectedDate = Date(sel)
            val local = selectedDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)

            formattedDate = when {
                local.isEqual(today) -> null
                local.isEqual(tomorrow) -> tomorrow.format(
                    DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                )
                else -> local.format(
                    DateTimeFormatter.ofPattern(
                        if (local.year == today.year) "MMM d" else "MMM d, yyyy",
                        Locale.getDefault()
                    )
                )
            }

            when {
                local.isEqual(today) -> {
                    binding.dateChip.text = getString(R.string.date)
                    binding.dateChip.isCloseIconVisible = false
                }
                local.isEqual(tomorrow) -> {
                    binding.dateChip.text = getString(R.string.date_tomorrow)
                    binding.dateChip.isCloseIconVisible = true
                }
                else -> {
                    binding.dateChip.text = formattedDate
                    binding.dateChip.isCloseIconVisible = true
                }
            }
        }
        picker.show(parentFragmentManager, "date")
    }

    private fun saveTask(task: TasksDatabase) {
        if (existingTaskId != null) tasksViewModel.updateTask(requireContext(), task)
        else tasksViewModel.addTask(requireContext(), task)
    }

    private fun scheduleNotification(selectedDate: Date?, selectedTime: String, taskId: String): Boolean {
        val parts = selectedTime.split(":")
        if (parts.size != 2) return false
        val hour = parts[0].toIntOrNull() ?: return false
        val minute = parts[1].toIntOrNull() ?: return false

        val dateForAlarm = selectedDate ?: Date().also {
            val todayLocal = LocalDate.now()
            formattedDate = todayLocal.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
        }

        val cal = Calendar.getInstance().apply {
            time = dateForAlarm
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (cal.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), getString(R.string.must_be_in_the_future), Toast.LENGTH_SHORT).show()
            return false
        }

        val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TITLE, taskText)
            putExtra(NotificationReceiver.EXTRA_TASK_ID, taskId)
        }
        val pi = PendingIntent.getBroadcast(
            requireContext(),
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            true
        } catch (_: SecurityException) {
            Toast.makeText(requireContext(), getString(R.string.grant_reminders_permission), Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            false
        }
    }
}
