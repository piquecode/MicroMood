package com.piquecode.micromood.util

import android.content.Context
import com.piquecode.micromood.R
import com.piquecode.micromood.data.Mood
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

object ChartConfiguration {
    
    fun LineChart.configure(noDataText: String, context: Context) {
        setDrawBorders(false)
        setNoDataText(noDataText)
        configureYAxis()
        xAxis.isEnabled = false
        legend.isEnabled = false
        description.isEnabled = false
        
        setTouchEnabled(true)
        setDragEnabled(false)
        setScaleEnabled(false)
        setPinchZoom(false)
        marker = DateMarkerView(context)
    }
    
    fun LineChart.addMoods(moods: List<Mood>, lineColor: Int) {
        val moodData = moods.map { Entry(it.date.time.toFloat(), it.getInvertedMood().toFloat()) }
        
        // Get colors for each mood point
        val circleColors = moods.map { mood ->
            when (mood.mood) {
                1 -> android.graphics.Color.parseColor("#A4D65E") // colorMood1
                2 -> android.graphics.Color.parseColor("#E0F05C") // colorMood2
                3 -> android.graphics.Color.parseColor("#FFD54F") // colorMood3
                4 -> android.graphics.Color.parseColor("#FFB74D") // colorMood4
                5 -> android.graphics.Color.parseColor("#FF8A65") // colorMood5
                else -> lineColor
            }
        }
        
        val data = LineDataSet(moodData, "").apply { 
            configureLineDataSet(lineColor, circleColors)
        }
        this.data = LineData(data).apply { setDrawValues(false) }
        animateY(600, Easing.EaseInBack)
    }
    
    private fun LineDataSet.configureLineDataSet(lineColor: Int, circleColors: List<Int>) {
        // Enable circles
        setDrawCircles(true)
        circleRadius = 4f
        circleHoleRadius = 2f
        setCircleColors(circleColors)
        circleHoleColor = android.graphics.Color.parseColor("#000000")
        
        // Line settings
        mode = LineDataSet.Mode.CUBIC_BEZIER
        cubicIntensity = 0.18f
        color = lineColor
        lineWidth = 3f
        
        // Highlight settings
        highLightColor = lineColor
        setDrawHighlightIndicators(false)
    }
    
    private fun LineChart.configureYAxis() {
        axisRight.isEnabled = false
        axisLeft.apply {
            axisMaximum = 6f
            axisMinimum = 0f
            granularity = 1f
            setLabelCount(4, false)
            setDrawAxisLine(false)
            setDrawLabels(false)
        }
    }
    
    private fun Mood.getInvertedMood() = 5 - mood + 1
}