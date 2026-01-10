package com.piquecode.micromood.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat.getColor
import com.piquecode.micromood.R
import com.piquecode.micromood.data.MoodRepository
import com.piquecode.micromood.moods.MainActivity
import com.piquecode.micromood.moods.MoodSelectionUseCase
import com.piquecode.micromood.util.Constants.ACTION_SET_MOOD
import com.piquecode.micromood.util.Constants.RC_SET_MOOD
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class MicroMoodWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob())

    @Inject
    lateinit var selectMood: MoodSelectionUseCase

    @Inject
    lateinit var repo: MoodRepository

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context ?: return
        appWidgetIds?.forEach {
            val view = getRemoteView(context)
            initialiseView(context, view)
            appWidgetManager?.updateAppWidget(it, view)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        if (context == null || intent == null) {
            return
        }
        
        if (intent.action in ACTION_SET_MOOD) {
            val mood = ACTION_SET_MOOD.indexOf(intent.action) + 1
            val view = getRemoteView(context)
            if (mood == -1) {
                return
            }
            scope.launch {
                selectMood(Date(), mood)
                updateWidget(view, context)
                AppWidgetManager
                    .getInstance(context)
                    .updateAppWidget(componentName(context), view)
            }
        }
    }

    private fun getRemoteView(context: Context) =
        RemoteViews(context.packageName, R.layout.layout_mood_row_widget)

    private fun initialiseView(context: Context, view: RemoteViews) {
        // Setup mood buttons
        moodButtons.forEachIndexed { i, res ->
            val intent = getMoodIntent(context, i)
            view.setOnClickPendingIntent(res, intent)
        }
        
        // Setup notes section click
        val notesIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("focus_notes", true)
        }
        val notesPendingIntent = PendingIntent.getActivity(
            context,
            RC_OPEN_NOTES,
            notesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        view.setOnClickPendingIntent(R.id.widget_notes_section, notesPendingIntent)
        
        scope.launch(Dispatchers.Main) {
            updateWidget(view, context)
        }
    }

    private suspend fun updateWidget(view: RemoteViews, context: Context) {
        val currentMood = repo.getMoodForDay(Date())
        
        // Update mood colors
        moodButtons.forEachIndexed { i, res ->
            val color = if (i + 1 == currentMood?.mood) moodColors[i] else R.color.colorDisabled
            view.setInt(res, "setColorFilter", getColor(context, color))
        }
        
        // Update notes indicator
        val notes = currentMood?.notes
        if (!notes.isNullOrBlank()) {
            view.setTextViewText(
                R.id.widget_notes_indicator,
                context.getString(R.string.note_saved, notes.take(30) + if (notes.length > 30) "..." else "")
            )
        } else {
            view.setTextViewText(R.id.widget_notes_indicator, context.getString(R.string.add_note))
        }
    }

    private fun getMoodIntent(context: Context, mood: Int): PendingIntent {
        return Intent(context, javaClass).let {
            it.action = ACTION_SET_MOOD[mood]
            PendingIntent.getBroadcast(
                context,
                RC_SET_MOOD,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    companion object {
        private const val RC_OPEN_NOTES = 1001
        
        private val moodColors = listOf(
            R.color.colorMood1,
            R.color.colorMood2,
            R.color.colorMood3,
            R.color.colorMood4,
            R.color.colorMood5
        )
        
        private val moodButtons = listOf(
            R.id.widget_mood_1,
            R.id.widget_mood_2,
            R.id.widget_mood_3,
            R.id.widget_mood_4,
            R.id.widget_mood_5
        )

        fun componentName(context: Context) =
            ComponentName(context, MicroMoodWidgetProvider::class.java)
    }
}