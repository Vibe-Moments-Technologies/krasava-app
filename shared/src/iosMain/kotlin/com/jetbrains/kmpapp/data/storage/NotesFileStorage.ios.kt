package com.jetbrains.kmpapp.data.storage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual open class NotesFileStorage {
    private fun filePath(): String? {
        val docs = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        ).firstOrNull() as? String ?: return null
        return "$docs/$FILE_NAME"
    }

    @OptIn(ExperimentalForeignApi::class)
    actual open fun read(): String? = try {
        val path = filePath() ?: return null
        val data = NSFileManager.defaultManager.contentsAtPath(path) as? NSData
        if (data == null) return null
        val size = data.length.toInt()
        if (size == 0) return ""
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            data.bytes?.let { src -> memcpy(pinned.addressOf(0), src, size.toULong()) }
        }
        bytes.decodeToString()
    } catch (_: Throwable) {
        null
    }

    @OptIn(ExperimentalForeignApi::class)
    actual open fun write(content: String): Boolean = try {        val path = filePath() ?: return false
        // Атомарно: временный файл + переименование. Обрыв процесса посреди
        // записи не оставляет наполовину стёртые заметки.
        val tmp = "$path.tmp"
        val bytes = content.encodeToByteArray()
        val data: NSData = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }
        NSFileManager.defaultManager.createFileAtPath(tmp, contents = data, attributes = null) &&
            NSFileManager.defaultManager.moveItemAtPath(tmp, toPath = path, error = null)
    } catch (_: Throwable) {
        false
    }

    actual open fun delete(): Boolean = try {
        val path = filePath() ?: return false
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    } catch (_: Throwable) {
        false
    }
}

private const val FILE_NAME = "lesson_notes.json"
