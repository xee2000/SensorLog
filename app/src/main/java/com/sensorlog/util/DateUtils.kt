package com.sensorlog.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    // API에 전달하는 ISO 8601 형식 (초 단위까지)
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    // 화면 표시용
    private val displayDateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
    private val displayDateTimeFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA)

    /** Date → ISO 8601 문자열 (API 전달용, 예: 2026-02-01T00:00:00) */
    fun toIso(date: Date): String = isoFormat.format(date)

    /** ISO 8601 문자열 → 화면 표시용 문자열 */
    fun formatTimestamp(isoTimestamp: String): String {
        return try {
            // "Z" suffix 또는 milliseconds가 붙어있어도 처리
            val cleaned = isoTimestamp.take(19)
            val date = isoFormat.parse(cleaned) ?: return isoTimestamp
            displayDateTimeFormat.format(date)
        } catch (e: Exception) {
            isoTimestamp
        }
    }

    /** Date → 화면 표시용 날짜 문자열 (예: 2026.02.01) */
    fun toDisplayDate(date: Date): String = displayDateFormat.format(date)

    /** 오늘 00:00:00 */
    fun todayStart(): Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time

    /** 오늘 23:59:59 */
    fun todayEnd(): Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 0)
    }.time

    /** N일 전 00:00:00 */
    fun daysAgo(days: Int): Date = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -days)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time

    /** year, month(0-based), day → 해당 날짜 00:00:00 */
    fun startOfDay(year: Int, month: Int, day: Int): Date = Calendar.getInstance().apply {
        set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0)
    }.time

    /** year, month(0-based), day → 해당 날짜 23:59:59 */
    fun endOfDay(year: Int, month: Int, day: Int): Date = Calendar.getInstance().apply {
        set(year, month, day, 23, 59, 59); set(Calendar.MILLISECOND, 0)
    }.time
}
