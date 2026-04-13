package br.com.anotacoes.domain.model

data class LocalTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
    }
}

sealed class TaskTime {

    abstract val isAllDay: Boolean
    abstract val startTime: LocalTime?
    abstract val endTime: LocalTime?

    data object AllDay : TaskTime() {
        override val isAllDay: Boolean = true
        override val startTime: LocalTime? = null
        override val endTime: LocalTime? = null
    }

    class Interval internal constructor(
        private val _startTime: LocalTime,
        private val _endTime: LocalTime
    ) : TaskTime() {
        override val isAllDay: Boolean = false
        override val startTime: LocalTime? get() = _startTime
        override val endTime: LocalTime? get() = _endTime

        init {
            val startTotal = _startTime.hour * 60 + _startTime.minute
            val endTotal = _endTime.hour * 60 + _endTime.minute
            require(endTotal > startTotal) {
                "End time must be after start time"
            }
        }
    }

    companion object {
        fun allDay(): AllDay = AllDay

        fun interval(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): Interval {
            return Interval(
                LocalTime(startHour, startMinute),
                LocalTime(endHour, endMinute)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this is AllDay && other is AllDay -> true
            this is Interval && other is Interval ->
                startTime == other.startTime && endTime == other.endTime
            else -> false
        }
    }

    override fun hashCode(): Int {
        return if (this is AllDay) 0 else startTime.hashCode() * 31 + endTime.hashCode()
    }
}
