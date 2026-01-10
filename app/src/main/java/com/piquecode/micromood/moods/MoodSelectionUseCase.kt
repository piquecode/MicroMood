package com.piquecode.micromood.moods

import com.piquecode.micromood.data.Mood
import com.piquecode.micromood.data.MoodRepository
import java.util.Date
import javax.inject.Inject

fun interface MoodSelectionUseCase {
    suspend operator fun invoke(date: Date, moodScore: Int)
}

class MoodSelectionUseCaseImpl @Inject constructor(
    private val repo: MoodRepository,
) : MoodSelectionUseCase {
    
    override suspend operator fun invoke(date: Date, moodScore: Int) {
        val existingMood = repo.getMoodForDay(date)
        
        if (existingMood?.mood == moodScore) {
            // Toggle off - remove mood
            repo.removeMood(date)
        } else {
            // Add or update mood, preserving existing notes
            val notesToKeep = existingMood?.notes
            repo.addMood(Mood(date, moodScore, notesToKeep))
        }
    }
}