package com.sensorlog.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val displayFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    private val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    private val timestampDisplayFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA)
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)

    fun formatForDisplay(date: Date): String = displayFormat.format(date)

    fun formatForApi(date: Date): String = apiFormat.format(date)

    fun parseApiDate(dateStr: String): Date? = try {
        apiFormat.parse(dateStr)
    } catch (e: Exception) {
        null
    }

    fun formatTimestamp(isoTimestamp: String): String {
        return try {
            val date = isoFormat.parse(isoTimestamp) ?: return isoTimestamp
            timestampDisplayFormat.format(date)
        } catch (e: Exception) {
            isoTimestamp
        }
    }

    fun todayStart(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun todayEnd(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.time
    }

    fun daysAgo(days: Int): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        return cal.time
    }

    fun calendarToApiString(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        return apiFormat.format(cal.time)
    }
}
