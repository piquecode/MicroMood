package com.piquecode.micromood.util

import android.content.Context
import android.widget.TextView
import com.piquecode.micromood.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

class DateMarkerView(context: Context) : MarkerView(context, R.layout.chart_marker_view) {
    
    private val dateText: TextView = findViewById(R.id.marker_date)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val date = Date(it.x.toLong())
            dateText.text = dateFormat.format(date)
        }
        super.refreshContent(e, highlight)
    }
    
    override fun getOffset(): MPPointF {
    return MPPointF(-(width / 2f) - 12f, -height.toFloat() - 8f)
    }
}