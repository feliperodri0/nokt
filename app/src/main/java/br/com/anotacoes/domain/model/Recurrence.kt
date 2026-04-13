package br.com.anotacoes.domain.model

import java.time.LocalDate
import java.time.DayOfWeek as JavaDayOfWeek

enum class RecurrenceType {
    Single,
    Daily,
    CustomWeekly,
    Monthly,
    Yearly
}

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    fun toJavaDayOfWeek(): JavaDayOfWeek = JavaDayOfWeek.valueOf(this.name)
}

sealed class Recurrence {
    abstract val type: RecurrenceType
    abstract val date: LocalDate?
    abstract val daysOfWeek: List<DayOfWeek>?
    abstract val dayOfMonth: Int?
    abstract val month: Int?

    data class Single(private val value: LocalDate) : Recurrence() {
        override val type: RecurrenceType = RecurrenceType.Single
        override val date: LocalDate = value
        override val daysOfWeek: List<DayOfWeek>? = null
        override val dayOfMonth: Int? = null
        override val month: Int? = null
    }

    object Daily : Recurrence() {
        override val type: RecurrenceType = RecurrenceType.Daily
        override val date: LocalDate? = null
        override val daysOfWeek: List<DayOfWeek>? = null
        override val dayOfMonth: Int? = null
        override val month: Int? = null
    }

    class CustomWeekly(days: List<DayOfWeek>) : Recurrence() {
        init {
            require(days.isNotEmpty()) {
                "Custom weekly recurrence must have at least one day of the week"
            }
        }

        override val type: RecurrenceType = RecurrenceType.CustomWeekly
        override val date: LocalDate? = null
        override val daysOfWeek: List<DayOfWeek>? = days.distinct()
        override val dayOfMonth: Int? = null
        override val month: Int? = null
    }

    // Repeats every month on the given day (1-31)
    data class Monthly(override val dayOfMonth: Int) : Recurrence() {
        init {
            require(dayOfMonth in 1..31) { "Day of month must be between 1 and 31" }
        }

        override val type: RecurrenceType = RecurrenceType.Monthly
        override val date: LocalDate? = null
        override val daysOfWeek: List<DayOfWeek>? = null
        override val month: Int? = null
    }

    // Repeats every year on the given month (1-12) and day (1-31)
    data class Yearly(val yearMonth: Int, val yearDay: Int) : Recurrence() {
        init {
            require(yearMonth in 1..12) { "Month must be between 1 and 12" }
            require(yearDay in 1..31) { "Day of month must be between 1 and 31" }
        }

        override val type: RecurrenceType = RecurrenceType.Yearly
        override val date: LocalDate? = null
        override val daysOfWeek: List<DayOfWeek>? = null
        override val dayOfMonth: Int? = yearDay
        override val month: Int? = yearMonth
    }

    companion object {
        fun single(date: LocalDate): Single {
            val today = LocalDate.now()
            val maxDate = today.plusYears(5)
            require(!date.isBefore(today) && !date.isAfter(maxDate)) {
                "Single recurrence date must be between today and 5 years in future"
            }
            return Single(date)
        }
        fun daily(): Daily = Daily
        fun customWeekly(days: List<DayOfWeek>): CustomWeekly = CustomWeekly(days)
        fun monthly(dayOfMonth: Int): Monthly = Monthly(dayOfMonth)
        fun yearly(month: Int, day: Int): Yearly = Yearly(month, day)
    }

    override fun toString(): String {
        return when (this) {
            is Single -> "Recurrence(Single, $date)"
            Daily -> "Recurrence(Daily, null)"
            is CustomWeekly -> "Recurrence(CustomWeekly, $daysOfWeek)"
            is Monthly -> "Recurrence(Monthly, day=$dayOfMonth)"
            is Yearly -> "Recurrence(Yearly, month=$yearMonth, day=$yearDay)"
        }
    }
}
