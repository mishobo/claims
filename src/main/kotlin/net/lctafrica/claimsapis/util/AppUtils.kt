package net.lctafrica.claimsapis.util

import java.time.LocalDate
import net.lctafrica.claimsapis.enums.CapitationPeriod


fun getDateRange(duration: CapitationPeriod): Pair<LocalDate, LocalDate> {
    val addMonths = when (duration) {
        CapitationPeriod.MONTHLY -> {
            1
        }

        CapitationPeriod.QUARTERLY -> {
            3
        }

        CapitationPeriod.SEMI_ANNUAL -> {
            6
        }

        else -> {
            12
        }
    }

    var monthBegin = LocalDate.now()
        .withDayOfMonth(1)
    var monthEnd = LocalDate.now().plusMonths(addMonths.toLong()).withDayOfMonth(1).minusDays(1)
    return Pair(monthBegin, monthEnd)
}


fun checkIfDateTodayIsInRange(startDate:LocalDate, endDate:LocalDate): Boolean{
    return (LocalDate.now().isEqual(startDate) || LocalDate.now().isEqual(endDate)) || (LocalDate.now().isAfter(startDate) && LocalDate.now().isBefore(endDate))
}