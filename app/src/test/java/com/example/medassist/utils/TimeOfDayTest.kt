package com.example.medassist.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeOfDayTest {

    @Test
    fun morning_runsFrom5amTo11am() {
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(5))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.fromHour(11))
    }

    @Test
    fun afternoon_runsFromNoonTo4pm() {
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.fromHour(12))
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.fromHour(16))
    }

    @Test
    fun evening_runsFrom5pmTo9pm() {
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(17))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.fromHour(21))
    }

    @Test
    fun lateNightAndEarlyMorning_useNeutralGreeting() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(22))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(0))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.fromHour(4))
    }
}
