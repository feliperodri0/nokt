package br.com.anotacoes.domain

import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.RecurrenceType
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {

    // ---- SINGLE ----

    @Test
    fun `single recurrence should accept date today`() {
        val today = LocalDate.now()
        val recurrence = Recurrence.single(today)
        assertThat(recurrence.type).isEqualTo(RecurrenceType.Single)
        assertThat(recurrence.date).isEqualTo(today)
    }

    @Test
    fun `single recurrence should accept date up to 5 years in future`() {
        val maxDate = LocalDate.now().plusYears(5)
        val recurrence = Recurrence.single(maxDate)
        assertThat(recurrence.type).isEqualTo(RecurrenceType.Single)
        assertThat(recurrence.date).isEqualTo(maxDate)
    }

    @Test
    fun `single recurrence should reject date in the past`() {
        val yesterday = LocalDate.now().minusDays(1)
        assertThrows(IllegalArgumentException::class.java) {
            Recurrence.single(yesterday)
        }
    }

    @Test
    fun `single recurrence should reject date beyond 5 years`() {
        val tooFar = LocalDate.now().plusYears(5).plusDays(1)
        assertThrows(IllegalArgumentException::class.java) {
            Recurrence.single(tooFar)
        }
    }

    @Test
    fun `single recurrence should accept date exactly at 5 year boundary`() {
        val boundary = LocalDate.now().plusYears(5)
        Recurrence.single(boundary) // should not throw
    }

    // ---- DAILY ----

    @Test
    fun `daily recurrence should not require days`() {
        val recurrence = Recurrence.daily()
        assertThat(recurrence.type).isEqualTo(RecurrenceType.Daily)
        assertThat(recurrence.daysOfWeek).isNull()
    }

    // ---- CUSTOM WEEKLY ----

    @Test
    fun `custom weekly recurrence should accept single day`() {
        val days = listOf(DayOfWeek.MONDAY)
        val recurrence = Recurrence.customWeekly(days)
        assertThat(recurrence.type).isEqualTo(RecurrenceType.CustomWeekly)
        assertThat(recurrence.daysOfWeek).isEqualTo(days)
    }

    @Test
    fun `custom weekly recurrence should accept multiple days`() {
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
        val recurrence = Recurrence.customWeekly(days)
        assertThat(recurrence.type).isEqualTo(RecurrenceType.CustomWeekly)
        assertThat(recurrence.daysOfWeek).isEqualTo(days)
    }

    @Test
    fun `custom weekly recurrence should reject empty days list`() {
        assertThrows(IllegalArgumentException::class.java) {
            Recurrence.customWeekly(emptyList())
        }
    }

    @Test
    fun `custom weekly recurrence should deduplicate days`() {
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        val recurrence = Recurrence.customWeekly(days)
        assertThat(recurrence.daysOfWeek?.distinct())
            .isEqualTo(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
    }

    // ---- MONTHLY ----

    @Test
    fun `monthly recurrence should accept valid day of month`() {
        val recurrence = Recurrence.monthly(17)
        assertThat(recurrence.type).isEqualTo(RecurrenceType.Monthly)
        assertThat(recurrence.dayOfMonth).isEqualTo(17)
        assertThat(recurrence.daysOfWeek).isNull()
        assertThat(recurrence.date).isNull()
    }

    @Test
    fun `monthly recurrence should accept boundary days 1 and 31`() {
        assertThat(Recurrence.monthly(1).dayOfMonth).isEqualTo(1)
        assertThat(Recurrence.monthly(31).dayOfMonth).isEqualTo(31)
    }

    @Test
    fun `monthly recurrence should reject day 0`() {
        assertThrows(IllegalArgumentException::class.java) { Recurrence.monthly(0) }
    }

    @Test
    fun `monthly recurrence should reject day 32`() {
        assertThrows(IllegalArgumentException::class.java) { Recurrence.monthly(32) }
    }

    @Test
    fun `monthly recurrence toString should include day`() {
        val r = Recurrence.monthly(17)
        assertThat(r.toString()).contains("Monthly")
        assertThat(r.toString()).contains("17")
    }

    // ---- YEARLY ----

    @Test
    fun `yearly recurrence should accept valid month and day`() {
        val recurrence = Recurrence.yearly(4, 24)
        assertThat(recurrence.type).isEqualTo(RecurrenceType.Yearly)
        assertThat(recurrence.month).isEqualTo(4)
        assertThat(recurrence.dayOfMonth).isEqualTo(24)
    }

    @Test
    fun `yearly recurrence should accept boundary months 1 and 12`() {
        assertThat(Recurrence.yearly(1, 1).month).isEqualTo(1)
        assertThat(Recurrence.yearly(12, 31).month).isEqualTo(12)
    }

    @Test
    fun `yearly recurrence should accept boundary days 1 and 31`() {
        assertThat(Recurrence.yearly(6, 1).dayOfMonth).isEqualTo(1)
        assertThat(Recurrence.yearly(6, 31).dayOfMonth).isEqualTo(31)
    }

    @Test
    fun `yearly recurrence should reject month 0`() {
        assertThrows(IllegalArgumentException::class.java) { Recurrence.yearly(0, 15) }
    }

    @Test
    fun `yearly recurrence should reject month 13`() {
        assertThrows(IllegalArgumentException::class.java) { Recurrence.yearly(13, 15) }
    }

    @Test
    fun `yearly recurrence should reject day 0`() {
        assertThrows(IllegalArgumentException::class.java) { Recurrence.yearly(6, 0) }
    }

    @Test
    fun `yearly recurrence should reject day 32`() {
        assertThrows(IllegalArgumentException::class.java) { Recurrence.yearly(6, 32) }
    }

    @Test
    fun `yearly recurrence should have null daysOfWeek and date`() {
        val recurrence = Recurrence.yearly(4, 24)
        assertThat(recurrence.daysOfWeek).isNull()
        assertThat(recurrence.date).isNull()
    }

    @Test
    fun `yearly recurrence toString should include month and day`() {
        val r = Recurrence.yearly(4, 24)
        assertThat(r.toString()).contains("Yearly")
        assertThat(r.toString()).contains("4")
        assertThat(r.toString()).contains("24")
    }

    @Test
    fun `yearly recurrence equality should work correctly`() {
        val r1 = Recurrence.yearly(4, 24)
        val r2 = Recurrence.yearly(4, 24)
        val r3 = Recurrence.yearly(5, 10)
        assertThat(r1).isEqualTo(r2)
        assertThat(r1).isNotEqualTo(r3)
    }

    // ---- UTILITIES ----

    @Test
    fun `recurrence should be equals correctly`() {
        val today = LocalDate.now()
        val r1 = Recurrence.single(today)
        val r2 = Recurrence.single(today)
        val r3 = Recurrence.daily()
        assertThat(r1).isEqualTo(r2)
        assertThat(r1).isNotEqualTo(r3)
    }

    @Test
    fun `recurrence should have correct toString for each type`() {
        val single = Recurrence.single(LocalDate.of(2026, 7, 15))
        val daily = Recurrence.daily()
        val customWeekly = Recurrence.customWeekly(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val yearly = Recurrence.yearly(4, 24)

        assertThat(single.toString()).contains("Single")
        assertThat(daily.toString()).contains("Daily")
        assertThat(customWeekly.toString()).contains("CustomWeekly")
        assertThat(yearly.toString()).contains("Yearly")
    }

    @Test
    fun `all recurrence types have correct month field`() {
        assertThat(Recurrence.single(LocalDate.now()).month).isNull()
        assertThat(Recurrence.daily().month).isNull()
        assertThat(Recurrence.customWeekly(listOf(DayOfWeek.MONDAY)).month).isNull()
        assertThat(Recurrence.monthly(15).month).isNull()
        assertThat(Recurrence.yearly(4, 24).month).isEqualTo(4)
    }
}
