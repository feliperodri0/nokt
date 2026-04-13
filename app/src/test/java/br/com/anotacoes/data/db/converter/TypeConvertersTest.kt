package br.com.anotacoes.data.db.converter

import br.com.anotacoes.domain.model.ChecklistItem
import br.com.anotacoes.domain.model.DayOfWeek
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for TypeConverters (Requirement 5).
 */
class TypeConvertersTest {

    private lateinit var converters: TypeConverters

    @Before
    fun setup() {
        converters = TypeConverters()
    }

    // --- LocalDate ---

    @Test
    fun `should convert LocalDate to String and back`() {
        val date = LocalDate.of(2026, 4, 15)

        val json = converters.toLocalDate(date)
        val restored = converters.fromLocalDate(json)

        assertThat(restored).isEqualTo(date)
    }

    @Test
    fun `LocalDate string format should be ISO format`() {
        val date = LocalDate.of(2026, 4, 15)

        val string = converters.toLocalDate(date)

        assertThat(string).isEqualTo("2026-04-15")
    }

    // --- List<DayOfWeek> ---

    @Test
    fun `should convert DayOfWeek list to JSON and back`() {
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val json = converters.fromDayOfWeekList(days)
        val restored = converters.toDayOfWeekList(json)

        assertThat(restored).isEqualTo(days)
    }

    @Test
    fun `should convert single DayOfWeek`() {
        val days = listOf(DayOfWeek.TUESDAY)

        val json = converters.fromDayOfWeekList(days)
        val restored = converters.toDayOfWeekList(json)

        assertThat(restored).containsExactly(DayOfWeek.TUESDAY)
    }

    @Test
    fun `DayOfWeek JSON should contain day names`() {
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)

        val json = converters.fromDayOfWeekList(days)

        assertThat(json).contains("MONDAY")
        assertThat(json).contains("FRIDAY")
    }

    // --- List<String> (attachments) ---

    @Test
    fun `should convert String list to JSON and back`() {
        val attachments = listOf("photo1.jpg", "doc.pdf")

        val json = converters.fromStringList(attachments)
        val restored = converters.toStringList(json)

        assertThat(restored).isEqualTo(attachments)
    }

    @Test
    fun `should handle empty String list`() {
        val json = converters.fromStringList(emptyList())
        val restored = converters.toStringList(json)

        assertThat(restored).isEmpty()
    }

    // --- List<ChecklistItem> ---

    @Test
    fun `should convert ChecklistItem list to JSON and back`() {
        val items = listOf(
            ChecklistItem(title = "Step 1", isChecked = false),
            ChecklistItem(title = "Step 2", isChecked = true)
        )

        val json = converters.fromChecklistItemList(items)
        val restored = converters.toChecklistItemList(json)

        assertThat(restored).hasSize(2)
        assertThat(restored[0].title).isEqualTo("Step 1")
        assertThat(restored[0].isChecked).isFalse()
        assertThat(restored[1].title).isEqualTo("Step 2")
        assertThat(restored[1].isChecked).isTrue()
    }

    @Test
    fun `ChecklistItem IDs should be preserved through conversion`() {
        val customId = "test-id-123"
        val items = listOf(ChecklistItem(id = customId, title = "Important"))

        val json = converters.fromChecklistItemList(items)
        val restored = converters.toChecklistItemList(json)

        assertThat(restored[0].id).isEqualTo(customId)
    }

    @Test
    fun `should handle empty ChecklistItem list`() {
        val json = converters.fromChecklistItemList(emptyList())
        val restored = converters.toChecklistItemList(json)

        assertThat(restored).isEmpty()
    }

    // --- List<Int> (advanceReminderDays) ---

    @Test
    fun `should convert Int list to JSON and back`() {
        val days = listOf(7, 3, 1)

        val json = converters.fromIntList(days)
        val restored = converters.toIntList(json)

        assertThat(restored).isEqualTo(days)
    }

    @Test
    fun `should handle empty Int list`() {
        val json = converters.fromIntList(emptyList())
        val restored = converters.toIntList(json)

        assertThat(restored).isEmpty()
    }

    @Test
    fun `should handle single value Int list`() {
        val json = converters.fromIntList(listOf(14))
        val restored = converters.toIntList(json)

        assertThat(restored).containsExactly(14)
    }

    @Test
    fun `Int list JSON should be parseable from raw string`() {
        val restored = converters.toIntList("[7,3,1]")

        assertThat(restored).containsExactly(7, 3, 1).inOrder()
    }

    @Test
    fun `empty bracket string should return empty Int list`() {
        val restored = converters.toIntList("[]")

        assertThat(restored).isEmpty()
    }
}
