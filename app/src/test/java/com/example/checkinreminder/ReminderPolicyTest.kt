package com.example.checkinreminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class ReminderPolicyTest {
    @Test
    fun morningBoundaries() {
        assertEquals(ReminderPeriod.NONE, ReminderPolicy.periodAt(LocalTime.of(6, 59, 59)))
        assertEquals(ReminderPeriod.MORNING, ReminderPolicy.periodAt(LocalTime.of(7, 0)))
        assertEquals(ReminderPeriod.MORNING, ReminderPolicy.periodAt(LocalTime.of(8, 59, 59)))
        assertEquals(ReminderPeriod.NONE, ReminderPolicy.periodAt(LocalTime.of(9, 0)))
    }

    @Test
    fun eveningBoundaries() {
        assertEquals(ReminderPeriod.NONE, ReminderPolicy.periodAt(LocalTime.of(17, 29, 59)))
        assertEquals(ReminderPeriod.EVENING, ReminderPolicy.periodAt(LocalTime.of(17, 30)))
        assertEquals(ReminderPeriod.EVENING, ReminderPolicy.periodAt(LocalTime.of(20, 59, 59)))
        assertEquals(ReminderPeriod.NONE, ReminderPolicy.periodAt(LocalTime.of(21, 0)))
    }
}
