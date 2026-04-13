package br.com.anotacoes.domain

import br.com.anotacoes.domain.model.ChecklistItem
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

/**
 * Tests for ChecklistItem model (Requirement 3 - sub-list).
 */
class ChecklistItemTest {

    @Test
    fun `should create checklist item with title`() {
        val item = ChecklistItem(title = "Buy milk")

        assertThat(item.title).isEqualTo("Buy milk")
        assertThat(item.isChecked).isFalse()
    }

    @Test
    fun `should assign id when none provided`() {
        val item1 = ChecklistItem(title = "Item 1")
        val item2 = ChecklistItem(title = "Item 2")

        assertThat(item1.id).isNotEmpty()
        assertThat(item2.id).isNotEmpty()
        assertThat(item1.id).isNotEqualTo(item2.id)
    }

    @Test
    fun `should accept custom id`() {
        val customId = UUID.randomUUID().toString()
        val item = ChecklistItem(id = customId, title = "Item")

        assertThat(item.id).isEqualTo(customId)
    }

    @Test
    fun `should reject empty title`() {
        assertThrows(IllegalArgumentException::class.java) {
            ChecklistItem(title = "")
        }

        assertThrows(IllegalArgumentException::class.java) {
            ChecklistItem(title = "   ")
        }
    }

    @Test
    fun `should toggle checked state`() {
        val item = ChecklistItem(title = "Item")
        assertThat(item.isChecked).isFalse()

        val toggled = item.toggle()
        assertThat(toggled.isChecked).isTrue()
        assertThat(toggled.id).isEqualTo(item.id)
    }
}
