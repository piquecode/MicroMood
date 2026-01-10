package com.piquecode.micromood.moods

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.piquecode.micromood.data.Mood
import com.piquecode.micromood.data.MoodRepository
import com.piquecode.micromood.data.PreferenceDao
import com.piquecode.micromood.dependencies.CoroutineDispatchers
import com.piquecode.micromood.util.DateHelpers.isSameDay
import com.piquecode.micromood.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MoodViewModel @Inject constructor(
    private val repo: MoodRepository,
    private val preferences: PreferenceDao,
    private val selectMood: MoodSelectionUseCase,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    val exportEvent = MutableLiveData<Event<String>?>()
    val importEvent = MutableLiveData<Event<ImportResult>?>()
    val selectedDate = MutableLiveData(Date())
    val shouldReportCrashes = preferences.shouldReportCrashes.asLiveData()
    val moods = selectedDate.switchMap { repo.getMoodsForMonth(it) }
    
    val currentMood = moods.map { monthMoods ->
        monthMoods
            .firstOrNull { mood -> selectedDate.value?.let { mood.date.isSameDay(it) } == true }
            ?.mood
    }

    val currentMoodWithNotes = moods.map { monthMoods ->
        monthMoods
            .firstOrNull { mood -> selectedDate.value?.let { mood.date.isSameDay(it) } == true }
    }

    fun toggleMood(moodScore: Int) {
        val date = selectedDate.value ?: Date()
        viewModelScope.launch {
            selectMood(date, moodScore)
        }
    }

    fun updateMoodNotes(date: Date, notes: String?) {
        viewModelScope.launch {
            val trimmedNotes = notes?.trim()?.takeIf { it.isNotEmpty() }
            repo.updateMoodNotes(date, trimmedNotes)
        }
    }

    fun export() {
        viewModelScope.launch {
            val moods = repo.getAllMoods()
            val header = "Date,Mood,Notes"
            val rows = moods.joinToString("\n") { 
                "${it.date},${it.mood},\"${it.notes ?: ""}\""
            }
            val data = "$header\n$rows"
            viewModelScope.launch(dispatchers.main) { 
                exportEvent.value = Event(data) 
            }
        }
    }

    fun importMoods(csvContent: String) {
        viewModelScope.launch {
            try {
                val importedMoods = parseCsv(csvContent)
                val existingMoods = repo.getAllMoods()
                val existingDates = existingMoods.map { it.date }.toSet()
                
                var addedCount = 0
                var skippedCount = 0
                
                importedMoods.forEach { mood ->
                    if (mood.date !in existingDates) {
                        repo.addMood(mood)
                        addedCount++
                    } else {
                        skippedCount++
                    }
                }
                
                viewModelScope.launch(dispatchers.main) {
                    importEvent.value = Event(ImportResult.Success(addedCount, skippedCount))
                }
            } catch (e: Exception) {
                viewModelScope.launch(dispatchers.main) {
                    importEvent.value = Event(ImportResult.Error(e.message ?: "Import failed"))
                }
            }
        }
    }

    private fun parseCsv(csvContent: String): List<Mood> {
    val lines = csvContent.trim().split("\n")
    if (lines.isEmpty()) return emptyList()
    
    val dataLines = if (lines[0].contains("Date") || lines[0].contains("Mood")) {
        lines.drop(1)
    } else {
        lines
    }
    
    return dataLines.mapNotNull { line ->
        try {
            // Parse CSV properly handling quoted fields
            val parts = parseCsvLine(line)
            if (parts.size >= 2) {
                val dateStr = parts[0].trim()
                val moodValue = parts[1].trim().toInt()
                val notes = if (parts.size >= 3) {
                    parts[2].trim()
                } else {
                    null
                }
                
                val date = parseDateString(dateStr)
                Mood(date, moodValue, notes)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    
    for (char in line) {
        when {
            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> {
                result.add(current.toString())
                current = StringBuilder()
            }
            else -> current.append(char)
        }
    }
    result.add(current.toString())
    return result
}

    private fun parseDateString(dateStr: String): Date {
        val formats = listOf(
            SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.US)
        )
        
        for (format in formats) {
            try {
                return format.parse(dateStr) ?: continue
            } catch (e: Exception) {
                continue
            }
        }
        
        throw IllegalArgumentException("Unable to parse date: $dateStr")
    }

    fun updateCrashReportingPreference(shouldReportCrashes: Boolean?) {
        viewModelScope.launch {
            preferences.updateCrashReporting(shouldReportCrashes)
        }
    }

    sealed class ImportResult {
        data class Success(val added: Int, val skipped: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}