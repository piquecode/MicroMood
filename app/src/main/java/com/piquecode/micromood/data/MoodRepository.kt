package com.piquecode.micromood.data

import androidx.lifecycle.LiveData
import com.piquecode.micromood.dependencies.CoroutineDispatchers
import com.piquecode.micromood.util.DateHelpers
import com.piquecode.micromood.util.DateHelpers.atStartOfDay
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

interface MoodRepository {
    suspend fun addMood(mood: Mood)
    suspend fun getMoodForDay(date: Date): Mood?
    suspend fun getAllMoods(): List<Mood>
    fun getMoodsForMonth(date: Date): LiveData<List<Mood>>
    suspend fun removeMood(date: Date)
    suspend fun updateMoodNotes(date: Date, notes: String?)
}

class MoodRepositoryImpl @Inject constructor(
    private val moods: MoodDao,
    private val dispatchers: CoroutineDispatchers
) : MoodRepository {

    override suspend fun addMood(mood: Mood) {
        val startOfDate = startOfDay(mood.date)
        val validatedMood = mood.copy(date = startOfDate)
        withContext(dispatchers.io) {
            moods.addMood(validatedMood)
        }
    }

    override suspend fun getMoodForDay(date: Date): Mood? {
        val startOfDate = startOfDay(date)
        return withContext(dispatchers.io) {
            moods.getMood(startOfDate.time)
        }
    }

    override suspend fun getAllMoods(): List<Mood> {
        return withContext(dispatchers.io) {
            moods.getAll()
        }
    }

    override fun getMoodsForMonth(date: Date): LiveData<List<Mood>> {
        val monthRange = DateHelpers.getMonthRange(date)
        return moods.getMoodsBetween(monthRange.first, monthRange.second)
    }

    override suspend fun removeMood(date: Date) {
        val startOfDate = startOfDay(date)
        return withContext(dispatchers.io) {
            moods.deleteMood(date = startOfDate.time)
        }
    }

    override suspend fun updateMoodNotes(date: Date, notes: String?) {
        val startOfDate = startOfDay(date)
        withContext(dispatchers.io) {
            moods.updateNotes(date = startOfDate.time, notes = notes)
        }
    }

    private fun startOfDay(date: Date): Date {
        return Calendar.getInstance()
            .apply { time = date }
            .atStartOfDay()
            .time
    }
}