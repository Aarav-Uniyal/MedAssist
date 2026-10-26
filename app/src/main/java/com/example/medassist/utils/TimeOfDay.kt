package com.example.medassist.utils

/** Used to pick the greeting on the Home screen. Pure logic, so it is easy to unit test. */
enum class TimeOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT;

    companion object {
        /** [hour] is 0..23 in the phone's local time. */
        fun fromHour(hour: Int): TimeOfDay = when (hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..21 -> EVENING
            else -> NIGHT
        }
    }
}
