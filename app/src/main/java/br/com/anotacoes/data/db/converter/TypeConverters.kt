package br.com.anotacoes.data.db.converter

import androidx.room.TypeConverter
import br.com.anotacoes.domain.model.ChecklistItem
import br.com.anotacoes.domain.model.DayOfWeek
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalTime

class TypeConverters {

    private val gson = Gson()

    // --- LocalDate ---
    @TypeConverter
    fun fromLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun toLocalDate(value: LocalDate): String = value.toString()

    // --- LocalTime ---
    @TypeConverter
    fun fromLocalTime(value: String): LocalTime = LocalTime.parse(value)

    @TypeConverter
    fun toLocalTime(value: LocalTime): String = value.toString()

    // --- List<DayOfWeek> ---
    @TypeConverter
    fun fromDayOfWeekList(days: List<DayOfWeek>): String = gson.toJson(days.map { it.name })

    @TypeConverter
    fun toDayOfWeekList(json: String): List<DayOfWeek> {
        val type = object : TypeToken<List<String>>() {}.type
        val names: List<String> = gson.fromJson(json, type)
        return names.map { DayOfWeek.valueOf(it) }
    }

    // --- List<String> (attachments) ---
    @TypeConverter
    fun fromStringList(list: List<String>): String = gson.toJson(list)

    @TypeConverter
    fun toStringList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // --- List<Int> (advance reminder days) ---
    @TypeConverter
    fun fromIntList(list: List<Int>): String = gson.toJson(list)

    @TypeConverter
    fun toIntList(json: String): List<Int> {
        if (json.isBlank() || json == "[]") return emptyList()
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // --- List<ChecklistItem> ---
    @TypeConverter
    fun fromChecklistItemList(items: List<ChecklistItem>): String = gson.toJson(items)

    @TypeConverter
    fun toChecklistItemList(json: String): List<ChecklistItem> {
        val type = object : TypeToken<List<ChecklistItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
