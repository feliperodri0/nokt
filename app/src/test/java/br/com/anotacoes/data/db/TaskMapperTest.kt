package br.com.anotacoes.data.db

import br.com.anotacoes.data.db.converter.TypeConverters
import br.com.anotacoes.domain.model.ChecklistItem
import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.domain.model.Recurrence
import br.com.anotacoes.domain.model.Task
import br.com.anotacoes.domain.model.TaskTime
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for Task entity ↔ domain model mapping (Requirement 1-5).
 */
class TaskMapperTest {

    private lateinit var converters: TypeConverters

    @Before
    fun setup() {
        converters = TypeConverters()
    }

    // --- Task.toEntity ---

    @Test
    fun `should map all-day task to entity`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-1",
            title = "Dentist",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(today)
        )

        val entity = task.toEntity(converters)

        assertThat(entity.id).isEqualTo("uuid-1")
        assertThat(entity.title).isEqualTo("Dentist")
        assertThat(entity.isAllDay).isTrue()
        assertThat(entity.startHour).isNull()
        assertThat(entity.startMinute).isNull()
        assertThat(entity.endHour).isNull()
        assertThat(entity.endMinute).isNull()
        assertThat(entity.recurrenceType).isEqualTo("SINGLE")
        assertThat(entity.recurrenceDaysJson).isEqualTo("[]")
    }

    @Test
    fun `should map time-interval task to entity`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-2",
            title = "Lunch",
            date = today,
            taskTime = TaskTime.interval(12, 30, 13, 15),
            recurrence = Recurrence.single(today)
        )

        val entity = task.toEntity(converters)

        assertThat(entity.isAllDay).isFalse()
        assertThat(entity.startHour).isEqualTo(12)
        assertThat(entity.startMinute).isEqualTo(30)
        assertThat(entity.endHour).isEqualTo(13)
        assertThat(entity.endMinute).isEqualTo(15)
    }

    @Test
    fun `should map task with description and attachments to entity`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-3",
            title = "Report",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(today),
            description = "Quarterly report",
            attachments = listOf("report.pdf")
        )

        val entity = task.toEntity(converters)

        assertThat(entity.description).isEqualTo("Quarterly report")
        assertThat(entity.attachmentsJson).contains("report.pdf")
    }

    @Test
    fun `should map task with checklist to entity`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-4",
            title = "Shopping",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(today),
            checklistItems = listOf(ChecklistItem(title = "Milk", isChecked = false))
        )

        val entity = task.toEntity(converters)

        assertThat(entity.checklistJson).contains("Milk")
    }

    @Test
    fun `should map daily recurrence to entity`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-5",
            title = "Exercise",
            date = today,
            taskTime = TaskTime.interval(7, 0, 7, 30),
            recurrence = Recurrence.daily()
        )

        val entity = task.toEntity(converters)

        assertThat(entity.recurrenceType).isEqualTo("DAILY")
    }

    @Test
    fun `should map custom weekly recurrence to entity`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-6",
            title = "Study",
            date = today,
            taskTime = TaskTime.interval(18, 0, 19, 0),
            recurrence = Recurrence.customWeekly(
                listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            )
        )

        val entity = task.toEntity(converters)

        assertThat(entity.recurrenceType).isEqualTo("CUSTOM_WEEKLY")
        assertThat(entity.recurrenceDaysJson).contains("MONDAY")
        assertThat(entity.recurrenceDaysJson).contains("FRIDAY")
    }

    @Test
    fun `should map single recurrence date to entity`() {
        val date = LocalDate.of(2026, 6, 15)
        val task = Task(
            id = "uuid-7",
            title = "Meeting",
            date = date,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(date)
        )

        val entity = task.toEntity(converters)

        assertThat(entity.recurrenceDateMillis).isNotNull()
    }

    // --- TaskEntity.toDomain ---

    @Test
    fun `should map all-day entity back to domain task`() {
        val today = LocalDate.now()
        val entity = TaskEntity(
            id = "uuid-1",
            title = "Dentist",
            dateMillis = today.toEpochDay() * 86400000,
            isAllDay = true,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            recurrenceType = "SINGLE",
            recurrenceDateMillis = today.toEpochDay() * 86400000,
            recurrenceDaysJson = "[]",
            description = null,
            attachmentsJson = "[]",
            checklistJson = "[]"
        )

        val task = entity.toDomain(converters)

        assertThat(task.title).isEqualTo("Dentist")
        assertThat(task.taskTime.isAllDay).isTrue()
        assertThat(task.taskTime.startTime).isNull()
    }

    @Test
    fun `should map time-interval entity back to domain task`() {
        val today = LocalDate.now()
        val entity = TaskEntity(
            id = "uuid-2",
            title = "Lunch",
            dateMillis = today.toEpochDay() * 86400000,
            isAllDay = false,
            startHour = 12,
            startMinute = 30,
            endHour = 13,
            endMinute = 15,
            recurrenceType = "SINGLE",
            recurrenceDateMillis = null,
            recurrenceDaysJson = "[]",
            description = null,
            attachmentsJson = "[]",
            checklistJson = "[]"
        )

        val task = entity.toDomain(converters)

        assertThat(task.taskTime.isAllDay).isFalse()
        assertThat(task.taskTime.startTime?.hour).isEqualTo(12)
        assertThat(task.taskTime.startTime?.minute).isEqualTo(30)
        assertThat(task.taskTime.endTime?.hour).isEqualTo(13)
        assertThat(task.taskTime.endTime?.minute).isEqualTo(15)
    }

    @Test
    fun `should map custom weekly entity back to domain task`() {
        val today = LocalDate.now()
        val entity = TaskEntity(
            id = "uuid-3",
            title = "Study",
            dateMillis = today.toEpochDay() * 86400000,
            isAllDay = true,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            recurrenceType = "CUSTOM_WEEKLY",
            recurrenceDateMillis = null,
            recurrenceDaysJson = converters.fromDayOfWeekList(
                listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
            ),
            description = "Study group",
            attachmentsJson = "[]",
            checklistJson = "[]"
        )

        val task = entity.toDomain(converters)

        assertThat(task.recurrence.type).isEqualTo(br.com.anotacoes.domain.model.RecurrenceType.CustomWeekly)
        assertThat(task.recurrence.daysOfWeek).hasSize(2)
    }

    @Test
    fun `should map entity with checklist back to domain task`() {
        val today = LocalDate.now()
        val checklistJson = converters.fromChecklistItemList(
            listOf(
                ChecklistItem(id = "item-1", title = "Read chapter", isChecked = true),
                ChecklistItem(id = "item-2", title = "Do exercises", isChecked = false)
            )
        )

        val entity = TaskEntity(
            id = "uuid-4",
            title = "Homework",
            dateMillis = today.toEpochDay() * 86400000,
            isAllDay = true,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            recurrenceType = "SINGLE",
            recurrenceDateMillis = null,
            recurrenceDaysJson = "[]",
            description = null,
            attachmentsJson = "[]",
            checklistJson = checklistJson
        )

        val task = entity.toDomain(converters)

        assertThat(task.checklistItems).hasSize(2)
        assertThat(task.checklistItems[0].title).isEqualTo("Read chapter")
        assertThat(task.checklistItems[0].isChecked).isTrue()
        assertThat(task.checklistItems[1].id).isEqualTo("item-2")
    }

    @Test
    fun `should map entity with attachments back to domain task`() {
        val today = LocalDate.now()
        val entity = TaskEntity(
            id = "uuid-5",
            title = "Trip",
            dateMillis = today.toEpochDay() * 86400000,
            isAllDay = true,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            recurrenceType = "SINGLE",
            recurrenceDateMillis = null,
            recurrenceDaysJson = "[]",
            description = null,
            attachmentsJson = converters.fromStringList(listOf("photo.jpg")),
            checklistJson = "[]"
        )

        val task = entity.toDomain(converters)

        assertThat(task.attachments).containsExactly("photo.jpg")
    }

    // --- Roundtrip ---

    @Test
    fun `task to entity to task roundtrip should preserve all-day task`() {
        val today = LocalDate.now()
        val original = Task(
            id = "roundtrip-1",
            title = "Birthday party",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(today),
            description = "Bring gift",
            attachments = emptyList(),
            checklistItems = listOf(ChecklistItem(title = "Buy gift"))
        )

        val entity = original.toEntity(converters)
        val restored = entity.toDomain(converters)

        assertThat(restored.id).isEqualTo(original.id)
        assertThat(restored.title).isEqualTo(original.title)
        assertThat(restored.date).isEqualTo(original.date)
        assertThat(restored.taskTime.isAllDay).isTrue()
        assertThat(restored.description).isEqualTo("Bring gift")
        assertThat(restored.checklistItems).hasSize(1)
    }

    @Test
    fun `task to entity to task roundtrip should preserve time-interval task`() {
        val today = LocalDate.now()
        val original = Task(
            id = "roundtrip-2",
            title = "Dentist",
            date = today,
            taskTime = TaskTime.interval(14, 0, 15, 30),
            recurrence = Recurrence.single(today)
        )

        val entity = original.toEntity(converters)
        val restored = entity.toDomain(converters)

        assertThat(restored.taskTime.isAllDay).isFalse()
        assertThat(restored.taskTime.startTime?.hour).isEqualTo(14)
        assertThat(restored.taskTime.endTime?.hour).isEqualTo(15)
    }

    @Test
    fun `task to entity to task roundtrip should preserve custom weekly`() {
        val today = LocalDate.now()
        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val original = Task(
            id = "roundtrip-3",
            title = "Gym",
            date = today,
            taskTime = TaskTime.interval(18, 0, 19, 0),
            recurrence = Recurrence.customWeekly(days)
        )

        val entity = original.toEntity(converters)
        val restored = entity.toDomain(converters)

        assertThat(restored.recurrence.type).isEqualTo(br.com.anotacoes.domain.model.RecurrenceType.CustomWeekly)
        assertThat(restored.recurrence.daysOfWeek).isEqualTo(days)
    }

    // --- YEARLY recurrence ---

    @Test
    fun `should map yearly recurrence to entity with MM-DD format`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-yearly-1",
            title = "Anniversary",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Yearly(4, 24)
        )

        val entity = task.toEntity(converters)

        assertThat(entity.recurrenceType).isEqualTo("YEARLY")
        assertThat(entity.recurrenceDaysJson).isEqualTo("4-24")
        assertThat(entity.recurrenceDateMillis).isNull()
    }

    @Test
    fun `should map yearly entity back to domain with correct month and day`() {
        val today = LocalDate.now()
        val entity = TaskEntity(
            id = "uuid-yearly-2",
            title = "Birthday",
            dateMillis = today.toEpochDay() * 86400000,
            isAllDay = true,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            recurrenceType = "YEARLY",
            recurrenceDateMillis = null,
            recurrenceDaysJson = "4-24",
            description = null,
            attachmentsJson = "[]",
            checklistJson = "[]"
        )

        val task = entity.toDomain(converters)

        assertThat(task.recurrence.type).isEqualTo(br.com.anotacoes.domain.model.RecurrenceType.Yearly)
        assertThat(task.recurrence.month).isEqualTo(4)
        assertThat(task.recurrence.dayOfMonth).isEqualTo(24)
    }

    @Test
    fun `task to entity to task roundtrip should preserve yearly recurrence`() {
        val today = LocalDate.now()
        val original = Task(
            id = "roundtrip-yearly",
            title = "Christmas",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Yearly(12, 25)
        )

        val entity = original.toEntity(converters)
        val restored = entity.toDomain(converters)

        assertThat(restored.recurrence.type).isEqualTo(br.com.anotacoes.domain.model.RecurrenceType.Yearly)
        assertThat(restored.recurrence.month).isEqualTo(12)
        assertThat(restored.recurrence.dayOfMonth).isEqualTo(25)
    }

    @Test
    fun `yearly recurrence stores boundary values correctly`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-yearly-boundary",
            title = "Jan 1st",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.Yearly(1, 1)
        )

        val entity = task.toEntity(converters)

        assertThat(entity.recurrenceDaysJson).isEqualTo("1-1")
        val restored = entity.toDomain(converters)
        assertThat(restored.recurrence.month).isEqualTo(1)
        assertThat(restored.recurrence.dayOfMonth).isEqualTo(1)
    }

    // --- advanceReminderDays mapping ---

    @Test
    fun `should map task with advance reminder days to entity`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-advance-1",
            title = "Dentist",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(today),
            advanceReminderDays = listOf(7, 3, 1)
        )

        val entity = task.toEntity(converters)

        assertThat(entity.advanceReminderDaysJson).contains("7")
        assertThat(entity.advanceReminderDaysJson).contains("3")
        assertThat(entity.advanceReminderDaysJson).contains("1")
    }

    @Test
    fun `should map task with empty advance reminder days to entity`() {
        val today = LocalDate.now()
        val task = Task(
            id = "uuid-advance-2",
            title = "Simple",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(today),
            advanceReminderDays = emptyList()
        )

        val entity = task.toEntity(converters)

        assertThat(entity.advanceReminderDaysJson).isEqualTo("[]")
    }

    @Test
    fun `task to entity to task roundtrip should preserve advance reminder days`() {
        val today = LocalDate.now()
        val original = Task(
            id = "roundtrip-advance",
            title = "Conference",
            date = today,
            taskTime = TaskTime.allDay(),
            recurrence = Recurrence.single(today),
            advanceReminderDays = listOf(5, 2)
        )

        val entity = original.toEntity(converters)
        val restored = entity.toDomain(converters)

        assertThat(restored.advanceReminderDays).containsExactly(5, 2).inOrder()
    }

    @Test
    fun `should parse advance reminder days from entity json`() {
        val today = LocalDate.now()
        val entity = TaskEntity(
            id = "uuid-advance-3",
            title = "Trip",
            dateMillis = today.toEpochDay() * 86400000,
            isAllDay = true,
            startHour = null,
            startMinute = null,
            endHour = null,
            endMinute = null,
            recurrenceType = "SINGLE",
            recurrenceDateMillis = null,
            recurrenceDaysJson = "[]",
            description = null,
            attachmentsJson = "[]",
            checklistJson = "[]",
            advanceReminderDaysJson = "[7,3,1]"
        )

        val task = entity.toDomain(converters)

        assertThat(task.advanceReminderDays).containsExactly(7, 3, 1).inOrder()
    }
}
