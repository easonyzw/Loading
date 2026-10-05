package com.example.checkinreminder

import java.time.LocalTime

enum class ReminderPeriod { MORNING, EVENING, NONE }

object ReminderPolicy {
    private val morningStart = LocalTime.of(7, 0)
    private val morningEnd = LocalTime.of(9, 0)
    private val eveningStart = LocalTime.of(17, 30)
    private val eveningEnd = LocalTime.of(21, 0)

    fun periodAt(time: LocalTime): ReminderPeriod = when {
        !time.isBefore(morningStart) && time.isBefore(morningEnd) -> ReminderPeriod.MORNING
        !time.isBefore(eveningStart) && time.isBefore(eveningEnd) -> ReminderPeriod.EVENING
        else -> ReminderPeriod.NONE
    }
}
