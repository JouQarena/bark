package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AttachmentType
import com.example.data.model.MessageStatus
import com.example.data.model.StoryType

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(separator = ";;;") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(";;;").filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = runCatching {
        MessageStatus.valueOf(value)
    }.getOrDefault(MessageStatus.SENT)

    @TypeConverter
    fun fromAttachmentType(type: AttachmentType): String = type.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType = runCatching {
        AttachmentType.valueOf(value)
    }.getOrDefault(AttachmentType.NONE)

    @TypeConverter
    fun fromStoryType(type: StoryType): String = type.name

    @TypeConverter
    fun toStoryType(value: String): StoryType = runCatching {
        StoryType.valueOf(value)
    }.getOrDefault(StoryType.TEXT)
}
