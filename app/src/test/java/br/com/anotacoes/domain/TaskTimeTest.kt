package br.com.anotacoes.domain

import br.com.anotacoes.domain.model.TaskTime
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Tests for TaskTime model (Requirement 2).
 * TaskTime can be "All day" OR a specific time interval (start - end).
 */
class TaskTimeTest {

    @Test
    fun `all day time should have no start or end`() {
        val allDay = TaskTime.allDay()

        assertThat(allDay.isAllDay).isTrue()
        assertThat(allDay.startTime).isNull()
        assertThat(allDay.endTime).isNull()
    }

    @Test
    fun `time interval should have valid start and end`() {
        val interval = TaskTime.interval(8, 0, 10, 0)

        assertThat(interval.isAllDay).isFalse()
        assertThat(interval.startTime?.hour).isEqualTo(8)
        assertThat(interval.startTime?.minute).isEqualTo(0)
        assertThat(interval.endTime?.hour).isEqualTo(10)
        assertThat(interval.endTime?.minute).isEqualTo(0)
    }

    @Test
    fun `time interval should not allow end before or equal to start`() {
        assertThrows(IllegalArgumentException::class.java) {
            TaskTime.interval(10, 0, 8, 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            TaskTime.interval(10, 0, 10, 0)
        }
    }

    @Test
    fun `time interval should validate minute range 0-59`() {
        // Valid minutes - should not throw
        TaskTime.interval(8, 0, 10, 59)
        TaskTime.interval(8, 30, 10, 59)

        // Invalid minutes
        assertThrows(IllegalArgumentException::class.java) {
            TaskTime.interval(8, 60, 10, 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            TaskTime.interval(8, 0, 10, -1)
        }
    }

    @Test
    fun `time interval should validate hour range 0-23`() {
        // Valid hours
        TaskTime.interval(0, 0, 23, 59)

        // Invalid hours
        assertThrows(IllegalArgumentException::class.java) {
            TaskTime.interval(24, 0, 10, 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            TaskTime.interval(-1, 0, 10, 0)
        }
    }

    @Test
    fun `equals should work correctly for all day`() {
        val day1 = TaskTime.allDay()
        val day2 = TaskTime.allDay()

        assertThat(day1).isEqualTo(day2)
    }

    @Test
    fun `equals should work correctly for time interval`() {
        val t1 = TaskTime.interval(8, 0, 10, 0)
        val t2 = TaskTime.interval(8, 0, 10, 0)
        val t3 = TaskTime.interval(9, 0, 10, 0)

        assertThat(t1).isEqualTo(t2)
        assertThat(t1).isNotEqualTo(t3)
    }
}
