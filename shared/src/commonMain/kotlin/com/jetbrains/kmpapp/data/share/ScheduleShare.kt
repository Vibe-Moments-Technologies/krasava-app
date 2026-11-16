package com.jetbrains.kmpapp.data.share

import com.jetbrains.kmpapp.data.model.UserLesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Buffer
import okio.BufferedSource
import okio.GzipSink
import okio.GzipSource
import okio.buffer

private const val BASE64_URL_ALPHABET =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

private const val MAX_JSON_BYTES = 8L * 1024 * 1024

@Serializable
data class SharedSchedule(
    val version: Int = CURRENT_VERSION,
    val name: String,
    val lessons: List<UserLesson>
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

sealed interface ScheduleShareRequest {
    val requestId: Long

    data class Confirm(
        val schedule: SharedSchedule,
        override val requestId: Long
    ) : ScheduleShareRequest

    data class Error(
        val message: String,
        override val requestId: Long
    ) : ScheduleShareRequest
}

object ScheduleShareInbox {
    private var nextRequestId = 0L

    private val _request = MutableStateFlow<ScheduleShareRequest?>(null)
    val request: StateFlow<ScheduleShareRequest?> = _request.asStateFlow()

    fun submitLink(raw: String) {
        val request = ScheduleShareCodec.decode(raw).fold(
            onSuccess = { ScheduleShareRequest.Confirm(it, ++nextRequestId) },
            onFailure = {
                ScheduleShareRequest.Error(
                    message = "Не удалось прочитать ссылку. Проверьте, что она скопирована полностью.",
                    requestId = ++nextRequestId
                )
            }
        )
        _request.value = request
    }

    fun consume() {
        _request.value = null
    }
}

object ScheduleShareCodec {
    const val LINK_PREFIX = "krasava://my/"

    private const val RAW_PREFIX = "krasava/my/"
    private const val SCHEME_PREFIX = "krasava:my/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun encode(schedule: SharedSchedule): String {
        val source = Buffer().writeUtf8(json.encodeToString(SharedSchedule.serializer(), schedule))
        val compressed = Buffer()
        val sink = GzipSink(compressed).buffer()
        try {
            sink.writeAll(source)
        } finally {
            sink.close()
        }
        return LINK_PREFIX + compressed.readByteArray().toBase64Url()
    }

    fun decode(raw: String): Result<SharedSchedule> = runCatching {
        val bytes = payload(raw).decodeBase64Url()
        val source = GzipSource(Buffer().write(bytes)).buffer()
        val schedule = try {
            json.decodeFromString(SharedSchedule.serializer(), source.readLimitedJson())
        } finally {
            source.close()
        }
        schedule.validate()
    }

    private fun BufferedSource.readLimitedJson(): String {
        val output = Buffer()
        var total = 0L
        while (true) {
            val read = read(output, 8192L)
            if (read == -1L) break
            total += read
            require(total <= MAX_JSON_BYTES) { "Schedule payload is too large" }
        }
        return output.readUtf8()
    }

    private fun payload(raw: String): String {
        val trimmed = raw.trim()
        val withoutPrefix = when {
            trimmed.startsWith(LINK_PREFIX, ignoreCase = true) ->
                trimmed.substring(LINK_PREFIX.length)
            trimmed.startsWith(RAW_PREFIX, ignoreCase = true) ->
                trimmed.substring(RAW_PREFIX.length)
            trimmed.startsWith(SCHEME_PREFIX, ignoreCase = true) ->
                trimmed.substring(SCHEME_PREFIX.length)
            else -> throw IllegalArgumentException("Unknown schedule link")
        }
        return withoutPrefix
            .substringBefore('?')
            .substringBefore('#')
            .filterNot { it.isWhitespace() }
            .trimEnd('=')
            .also { require(it.isNotEmpty()) }
    }

    private fun SharedSchedule.validate(): SharedSchedule {
        require(version == SharedSchedule.CURRENT_VERSION)
        val cleanName = name.trim()
        require(cleanName.isNotEmpty() && cleanName.length <= 200)
        require(lessons.size <= 2000)
        lessons.forEach { lesson ->
            require(lesson.id.isNotBlank())
            require(lesson.subject.isNotBlank() && lesson.subject.length <= 300)
            require(lesson.bellNumber in 1..7)
            require(lesson.teacher.length <= 500)
            require(lesson.auditorium.length <= 500)
            require(lesson.groups.length <= 500)
            require(lesson.excludedDates.size <= 5000)
        }
        return copy(name = cleanName)
    }
}

private fun ByteArray.toBase64Url(): String {
    val output = StringBuilder((size * 4 + 2) / 3)
    var index = 0
    while (index < size) {
        val remaining = size - index
        val first = this[index].toInt() and 0xFF
        val second = if (remaining > 1) this[index + 1].toInt() and 0xFF else 0
        val third = if (remaining > 2) this[index + 2].toInt() and 0xFF else 0
        val chunk = (first shl 16) or (second shl 8) or third

        output.append(BASE64_URL_ALPHABET[(chunk ushr 18) and 0x3F])
        output.append(BASE64_URL_ALPHABET[(chunk ushr 12) and 0x3F])
        if (remaining > 1) output.append(BASE64_URL_ALPHABET[(chunk ushr 6) and 0x3F])
        if (remaining > 2) output.append(BASE64_URL_ALPHABET[chunk and 0x3F])
        index += 3
    }
    return output.toString()
}

private fun String.decodeBase64Url(): ByteArray {
    val output = ByteArray((length * 3) / 4 + 2)
    var outputLength = 0
    var buffer = 0
    var bits = 0

    for (character in this) {
        val value = when (character) {
            in 'A'..'Z' -> character - 'A'
            in 'a'..'z' -> character - 'a' + 26
            in '0'..'9' -> character - '0' + 52
            '-' -> 62
            '_' -> 63
            else -> throw IllegalArgumentException("Invalid Base64 character")
        }
        buffer = (buffer shl 6) or value
        bits += 6
        if (bits >= 8) {
            bits -= 8
            output[outputLength++] = ((buffer ushr bits) and 0xFF).toByte()
            buffer = buffer and ((1 shl bits) - 1)
        }
    }
    return output.copyOf(outputLength)
}
