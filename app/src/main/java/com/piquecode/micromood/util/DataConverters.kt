package com.piquecode.micromood.util

import androidx.room.TypeConverter
import java.util.*

class DataConverters {
    @TypeConverter fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    @TypeConverter fun toTimestamp(value: Date?): Long? = value?.time
}
