package com.piquecode.micromood.util

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.piquecode.micromood.R
import com.piquecode.micromood.data.Mood
import java.util.Calendar
import java.util.Date

/**
 * Weekly Summary utilities for MicroMood
 * Calculates streaks and weekly mood frequencies
 */
object WeeklySummaryHelper {

    /**
     * Calculate the current daily streak
     * A streak is broken if any day is missing a mood entry
     */
    fun calculateStreak(moods: List<Mood>): Int {
        if (moods.isEmpty()) return 0

        // Create a set of all dates with moods for O(1) lookup
        val datesWithMoods = moods.map { normalizeDate(it.date) }.toSet()
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Check if today has a mood, if not the streak is 0
        if (!datesWithMoods.contains(today.time)) {
            return 0
        }

        var streak = 0
        val currentDate = today.clone() as Calendar

        // Count consecutive days backwards from today
        while (datesWithMoods.contains(currentDate.time)) {
            streak++
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
        }

        return streak
    }

    /**
     * Get mood frequency for the current week (Monday to Sunday)
     * Returns a map of mood values to their counts
     */
    fun getWeeklyMoodFrequency(moods: List<Mood>): Map<Int, Int> {
        val calendar = Calendar.getInstance()
        
        // Get start of week (Monday)
        val startOfWeek = calendar.clone() as Calendar
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
        startOfWeek.set(Calendar.MINUTE, 0)
        startOfWeek.set(Calendar.SECOND, 0)
        startOfWeek.set(Calendar.MILLISECOND, 0)
        
        // Get end of week (Sunday)
        val endOfWeek = startOfWeek.clone() as Calendar
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6)
        endOfWeek.set(Calendar.HOUR_OF_DAY, 23)
        endOfWeek.set(Calendar.MINUTE, 59)
        endOfWeek.set(Calendar.SECOND, 59)

        val weekMoods = moods.filter { mood ->
            !mood.date.before(startOfWeek.time) && !mood.date.after(endOfWeek.time)
        }

        // Count frequency of each mood value
        return weekMoods.groupBy { it.mood }
            .mapValues { it.value.size }
    }

    /**
     * Update the weekly summary card UI
     */
    fun updateWeeklySummaryCard(
        view: View,
        moods: List<Mood>
    ) {
        // Update streak
        val streak = calculateStreak(moods)
        val streakCount = view.findViewById<TextView>(R.id.streak_count)
        streakCount.text = if (streak == 1) "1 day" else "$streak days"

        // Update weekly frequency
        val frequency = getWeeklyMoodFrequency(moods)
        updateMoodFrequency(view, frequency)
    }

    /**
     * Update mood frequency display
     */
    private fun updateMoodFrequency(view: View, frequency: Map<Int, Int>) {
        val emptyMessage = view.findViewById<TextView>(R.id.empty_week_message)
        
        if (frequency.isEmpty()) {
            // Show empty state
            emptyMessage.visibility = View.VISIBLE
            hideMoodRow(view, R.id.mood_1_row)
            hideMoodRow(view, R.id.mood_2_row)
            hideMoodRow(view, R.id.mood_3_row)
            hideMoodRow(view, R.id.mood_4_row)
            hideMoodRow(view, R.id.mood_5_row)
            return
        }

        emptyMessage.visibility = View.GONE

        // Update each mood row (1 = Great, 2 = Good, 3 = Okay, 4 = Bad, 5 = Terrible)
        updateMoodRow(view, 1, frequency[1] ?: 0, "feeling great")
        updateMoodRow(view, 2, frequency[2] ?: 0, "feeling good")
        updateMoodRow(view, 3, frequency[3] ?: 0, "feeling okay")
        updateMoodRow(view, 4, frequency[4] ?: 0, "feeling bad")
        updateMoodRow(view, 5, frequency[5] ?: 0, "feeling terrible")
    }

    /**
     * Update individual mood row
     */
    private fun updateMoodRow(view: View, moodValue: Int, count: Int, label: String) {
        val rowId = when (moodValue) {
            5 -> R.id.mood_5_row
            4 -> R.id.mood_4_row
            3 -> R.id.mood_3_row
            2 -> R.id.mood_2_row
            1 -> R.id.mood_1_row
            else -> return
        }

        val countId = when (moodValue) {
            5 -> R.id.mood_5_count
            4 -> R.id.mood_4_count
            3 -> R.id.mood_3_count
            2 -> R.id.mood_2_count
            1 -> R.id.mood_1_count
            else -> return
        }

        val labelId = when (moodValue) {
            5 -> R.id.mood_5_label
            4 -> R.id.mood_4_label
            3 -> R.id.mood_3_label
            2 -> R.id.mood_2_label
            1 -> R.id.mood_1_label
            else -> return
        }

        val row = view.findViewById<LinearLayout>(rowId)
        val countText = view.findViewById<TextView>(countId)
        val labelText = view.findViewById<TextView>(labelId)

        if (count > 0) {
            row.visibility = View.VISIBLE
            countText.text = count.toString()
            val dayWord = if (count == 1) "day" else "days"
            labelText.text = "$dayWord $label"
        } else {
            row.visibility = View.GONE
        }
    }

    /**
     * Hide a mood row
     */
    private fun hideMoodRow(view: View, rowId: Int) {
        view.findViewById<LinearLayout>(rowId).visibility = View.GONE
    }
    
    /**
     * Normalize a date to midnight for comparison
     */
    private fun normalizeDate(date: Date): Date {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }
}