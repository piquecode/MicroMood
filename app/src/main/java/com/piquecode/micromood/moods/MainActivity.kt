package com.piquecode.micromood.moods

import com.piquecode.micromood.util.WeeklySummaryHelper
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.piquecode.micromood.R
import com.piquecode.micromood.about.AboutActivity
import com.piquecode.micromood.data.Mood
import com.piquecode.micromood.databinding.ActivityMainBinding
import com.piquecode.micromood.util.ChartConfiguration.addMoods
import com.piquecode.micromood.util.ChartConfiguration.configure
import com.piquecode.micromood.util.Constants.TAG_DATE_PICKER
import com.piquecode.micromood.util.DarkModePreferenceWatcher
import com.piquecode.micromood.util.DateHelpers
import com.piquecode.micromood.util.DateHelpers.toCalendar
import com.piquecode.micromood.util.Event
import com.piquecode.micromood.util.ReminderManager
import com.piquecode.micromood.widget.MicroMoodWidgetProvider
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Date


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: MoodViewModel by viewModels()
    private val darkModePreferenceWatcher by lazy {
        DarkModePreferenceWatcher(this)
    }

    private val importCsvLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importCsvFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding
        .inflate(layoutInflater)
        .also {
            it.viewModel = viewModel
            it.lifecycleOwner = this
        }
    setContentView(binding.root)
    initialiseView()
    watchData()
    getDefaultSharedPreferences(this)
        .registerOnSharedPreferenceChangeListener(darkModePreferenceWatcher)
    
    if (intent?.getBooleanExtra("focus_notes", false) == true) {
        binding.notesInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(binding.notesInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
      }
      }

    override fun onDestroy() {
        super.onDestroy()

        getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(darkModePreferenceWatcher)
    }

    private fun initialiseView() {
        binding.settings.setOnClickListener { openAbout() }
        binding.date.setOnClickListener { showDatePicker() }
        binding.importButton.setOnClickListener { openImportPicker() }
        binding.reminderButton.setOnClickListener { showReminderDialog() }

        configureCalendar()

        binding.chart.configure(getString(R.string.no_moods), this)
        
        setupNotesInput()
    }

    private fun setupNotesInput() {
        binding.saveNotesButton.setOnClickListener {
            saveNotes()
        }
        
        binding.notesInput.setOnEditorActionListener { _, _, _ ->
            saveNotes()
            binding.notesInput.clearFocus()
            true
        }
    }

    private fun saveNotes() {
        val notes = binding.notesInput.text?.toString()
        val date = viewModel.selectedDate.value ?: Date()
        
        viewModel.currentMoodWithNotes.value?.let { existingMood ->
            viewModel.updateMoodNotes(date, notes)
            
            android.widget.Toast.makeText(
                this,
                "Note saved!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.notesInput.windowToken, 0)
            
            binding.notesInput.clearFocus()
        } ?: run {
            android.widget.Toast.makeText(
                this,
                "Please select a mood first",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun watchData() {
        viewModel.moods.observe(this) { moods ->
            updateCalendar(moods)
            binding.chart.addMoods(moods, getColor(this, R.color.colorPrimary))
            updateWeeklySummary(moods)
        }

        viewModel.selectedDate.observe(this) { 
            initialiseView()
            updateNotesForSelectedDate()
        }
        
        viewModel.exportEvent.observe(this) { exportMoodFile(it) }
        viewModel.importEvent.observe(this) { handleImportResult(it) }
        viewModel.currentMood.observe(this) { updateWidget() }
        
        viewModel.currentMoodWithNotes.observe(this) { mood ->
            binding.notesInput.setText(mood?.notes ?: "")
            binding.saveNotesButton.isEnabled = mood != null
        }
    }

    private fun updateWeeklySummary(moods: List<Mood>) {
        val card = findViewById<View>(R.id.card_weekly_summary)
        WeeklySummaryHelper.updateWeeklySummaryCard(card, moods)
    }

    private fun updateNotesForSelectedDate() {
        viewModel.currentMoodWithNotes.value?.let { mood ->
            binding.notesInput.setText(mood.notes ?: "")
        } ?: run {
            binding.notesInput.setText("")
        }
    }

    private fun showDatePicker() {
        val constraintsBuilder = com.google.android.material.datepicker.CalendarConstraints.Builder()
            .setValidator(com.google.android.material.datepicker.DateValidatorPointBackward.now())
        
        MaterialDatePicker.Builder
            .datePicker()
            .setSelection(viewModel.selectedDate.value?.time ?: System.currentTimeMillis())
            .setCalendarConstraints(constraintsBuilder.build())
            .build()
            .apply {
                addOnPositiveButtonClickListener {
                    viewModel.selectedDate.value = Date().apply { time = it }
                }
            }
            .show(supportFragmentManager, TAG_DATE_PICKER)
    }

    private fun configureCalendar() {
        val date = viewModel.selectedDate.value ?: return
        binding.calendar.apply {
            setDate(date.toCalendar())
            setMaximumDate(java.util.Calendar.getInstance())
            
            setOnDayClickListener(object : OnDayClickListener {
                override fun onDayClick(eventDay: EventDay) {
                    val selectedDate = eventDay.calendar.time
                    val today = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 23)
                        set(java.util.Calendar.MINUTE, 59)
                        set(java.util.Calendar.SECOND, 59)
                    }.time
                    
                    if (!selectedDate.after(today)) {
                        viewModel.selectedDate.value = selectedDate
                    } else {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "No magic crystal to predict the future!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })

            AppCompatResources.getDrawable(context, R.drawable.ic_right)
                ?.apply { setTint(getColor(context, R.color.colorIcon)) }
                ?.also { setForwardButtonImage(it) }

            AppCompatResources.getDrawable(context, R.drawable.ic_left)
                ?.apply { setTint(getColor(context, R.color.colorIcon)) }
                ?.also { setPreviousButtonImage(it) }

            setSwipeEnabled(false)

            this.findViewById<View>(R.id.previousButton).setOnClickListener {
                viewModel.selectedDate.value?.let {
                    viewModel.selectedDate.value = DateHelpers.getFirstDayOfPreviousMonth(it)
                }
            }

            this.findViewById<View>(R.id.forwardButton).setOnClickListener {
                viewModel.selectedDate.value?.let {
                    viewModel.selectedDate.value = DateHelpers.getFirstDayOfNextMonth(it)
                }
            }
        }
    }

    private fun updateCalendar(moods: List<Mood>) {
        val days = moods.map { mood ->
            CalendarDay(mood.date.toCalendar()).also { day ->
                day.backgroundDrawable = createCalendarDayDrawable(mood)
            }
        }

        binding.calendar.setCalendarDays(days)
    }

    private fun createCalendarDayDrawable(mood: Mood): android.graphics.drawable.Drawable? {
        val colorDrawable = getDrawable(this, R.drawable.calendar_event)
            ?.apply { setTint(getColor(this@MainActivity, mood.getMoodColor())) }
        
        if (mood.notes.isNullOrBlank()) {
            return colorDrawable
        }
        
        val pencilDrawable = getDrawable(this, R.drawable.ic_pencil)
            ?.apply { 
                setBounds(0, 0, 20, 20)
            }
        
        return android.graphics.drawable.LayerDrawable(
            arrayOf(colorDrawable, pencilDrawable)
        ).apply {
            setLayerInset(1, 48, 48, 0, 0)
        }
    }

    private fun exportMoodFile(event: Event<String>?) {
        createMoodFile(event)?.let { exportFile(it) }
    }

    private fun createMoodFile(event: Event<String>?): File? {
    return event?.unhandledData?.let { csvData ->
        val hasNotes = csvData.lines().drop(1).any { line ->
            if (line.isBlank()) return@any false
            val parts = line.split(",")
            if (parts.size < 3) return@any false
            val notes = parts[2].trim().removeSurrounding("\"")
            notes.isNotEmpty()
        }
        val notesLabel = if (hasNotes) "notes" else "nonotes"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        
        val exportsDir = File(filesDir, "exports").apply { mkdirs() }
        File(exportsDir, "MicroMood_${dateString}_${notesLabel}.csv").apply { 
            writeBytes(csvData.toByteArray()) 
        }
    }
}

    private fun exportFile(file: File) {
        val uri = FileProvider.getUriForFile(this, packageName, file)
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/csv"
            clipData = ClipData("Mood Export", arrayOf(type), ClipData.Item(uri))
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val shareIntent = Intent.createChooser(sendIntent, getString(R.string.export))
        startActivity(shareIntent)
    }

    private fun openImportPicker() {
        importCsvLauncher.launch("text/*")
    }

    private fun importCsvFromUri(uri: Uri) {
        try {
            val csvContent = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            
            if (csvContent != null) {
                viewModel.importMoods(csvContent)
            } else {
                android.widget.Toast.makeText(
                    this,
                    "Failed to read file",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                this,
                "Error reading file: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleImportResult(event: Event<MoodViewModel.ImportResult>?) {
    event?.unhandledData?.let { result ->
        when (result) {
            is MoodViewModel.ImportResult.Success -> {
                val message = buildString {
                    append("Import complete!\n")
                    append("Added: ${result.added} moods\n")
                    if (result.skipped > 0) {
                        append("Skipped: ${result.skipped} duplicates")
                    }
                }
                
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Import Successful")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
            is MoodViewModel.ImportResult.Error -> {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Import Failed")
                    .setMessage(result.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    }

    private fun updateWidget() {
        val widgetName = MicroMoodWidgetProvider.componentName(this)
        val ids = AppWidgetManager.getInstance(this).getAppWidgetIds(widgetName)
        val intent = Intent(this, MicroMoodWidgetProvider::class.java).also {
            it.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            it.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }

        sendBroadcast(intent)
    }

    private fun openAbout() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    private fun showReminderDialog() {
        val currentTime = ReminderManager.getReminderTime(this)
        val isEnabled = ReminderManager.isReminderEnabled(this)

        if (isEnabled) {
            showManageReminderDialog(currentTime)
        } else {
            showSetReminderDialog()
        }
    }

    private fun showSetReminderDialog() {
        val currentTime = ReminderManager.getReminderTime(this)
        val hour = currentTime?.first ?: 20
        val minute = currentTime?.second ?: 0

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Set Daily Reminder")
            .setMessage("Choose a time to be reminded to log your mood daily")
            .setPositiveButton("Choose Time") { _, _ ->
                showTimePicker(hour, minute)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManageReminderDialog(currentTime: Pair<Int, Int>?) {
        val hour = currentTime?.first ?: 20
        val minute = currentTime?.second ?: 0
        val timeString = String.format("%02d:%02d", hour, minute)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Daily Reminder")
            .setMessage("Current reminder time: $timeString")
            .setPositiveButton("Change Time") { _, _ ->
                showTimePicker(hour, minute)
        }
        .setNegativeButton("Delete") { _, _ ->
            showDeleteReminderConfirmation()
        }
        .setNeutralButton("Cancel", null)
        .show()
    }

    private fun showDeleteReminderConfirmation() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Delete Reminder?")
            .setMessage("Are you sure you want to delete your daily reminder?")
            .setPositiveButton("Delete") { _, _ ->
                ReminderManager.cancelReminder(this)
                android.widget.Toast.makeText(this, "Reminder deleted", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTimePicker(hour: Int, minute: Int) {
        val picker = android.app.TimePickerDialog(
            this,
            R.style.TimePickerTheme,
            { _, selectedHour, selectedMinute ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
                }
            }

            ReminderManager.scheduleReminder(this, selectedHour, selectedMinute)
            android.widget.Toast.makeText(
                this,
                "Reminder set for ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        },
        hour,
        minute,
        true
    )
    picker.show()
    }

    private fun Mood.getMoodColor() = when (mood) {
        1 -> R.color.colorMood1
        2 -> R.color.colorMood2
        3 -> R.color.colorMood3
        4 -> R.color.colorMood4
        5 -> R.color.colorMood5
        else -> R.color.colorBackground
    }
}