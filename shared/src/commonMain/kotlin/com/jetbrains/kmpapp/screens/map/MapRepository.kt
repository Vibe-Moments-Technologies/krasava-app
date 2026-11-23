package com.jetbrains.kmpapp.screens.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kmp_app_template.shared.generated.resources.Res

object MapRepository {
    private val svgCache = mutableMapOf<String, String>()
    private var allObjects: List<MapObject>? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun loadFloorSvg(campusId: String, floor: Int): String? = withContext(Dispatchers.IO) {
        // Карты МП-1 (репорт владельца): ПО УМОЛЧАНИЮ — новая съёмка
        // mp-1-new (инверсия исходного тумблера); старые карты mp-1
        // включаются тумблером «Старая съёмка МП-1» в отладке. Ключ кеша
        // различает версии, чтобы переключение не подмешивало этажи.
        val oldMp1 = com.jetbrains.kmpapp.data.DebugConfig.isOldMp1MapEnabled.value
        val cacheKey = "$campusId-$floor" + if (campusId == "mp-1" && oldMp1) "-old" else ""
        svgCache[cacheKey]?.let { return@withContext it }

        val path = floorPath(campusId, floor, oldMp1)
        if (path.isEmpty()) return@withContext null

        try {
            val bytes = Res.readBytes(path)
            val content = bytes.decodeToString()
            svgCache[cacheKey] = content
            content
        } catch (e: Exception) {
            println("Failed to load SVG for $path: ${e.message}")
            null
        }
    }

    suspend fun loadObjects(): List<MapObject> = withContext(Dispatchers.IO) {
        allObjects?.let { return@withContext it }

        try {
            val bytes = Res.readBytes("files/maps/objects.json")
            val container = json.decodeFromString<MapObjectsContainer>(bytes.decodeToString())
            allObjects = container.objects
            container.objects
        } catch (e: Exception) {
            println("Failed to load objects.json: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchRooms(query: String): List<RoomSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val objects = loadObjects()
        val queryLower = trimmed.lowercase()

        return objects
            .filter { it.type == "room" && it.name.lowercase().contains(queryLower) }
            .take(15)
            .map { obj ->
                val (campusId, campusName, floor) = guessCampusAndFloor(obj.name)
                RoomSearchResult(
                    id = obj.id,
                    name = obj.name,
                    type = obj.type,
                    campusId = campusId,
                    campusName = campusName,
                    floor = floor
                )
            }
    }

    /**
     * Реальный объём векторных карт в байтах.
     *
     * Экран «Данные и память» показывал «~2.2 МБ» — константу из вёрстки.
     * Здесь считаем фактические размеры SVG-ресурсов: те же пути, что
     * использует [loadFloorSvg], плюс индекс аудиторий objects.json.
     * Результат кэшируется: файлы в ресурсах не меняются в рантайме.
     */
    suspend fun mapsSizeBytes(): Long = withContext(Dispatchers.IO) {
        cachedMapsSize?.let { return@withContext it }
        var total = 0L
        // Обе съёмки МП-1 лежат в приложении, поэтому обе и учитываем.
        val paths = buildList {
            add("files/maps/objects.json")
            CAMPUSES.forEach { campus ->
                campus.floors.forEach { floor ->
                    add(floorPath(campus.id, floor, oldMp1 = false))
                    if (campus.id == "mp-1") add(floorPath(campus.id, floor, oldMp1 = true))
                }
            }
        }
        paths.forEach { path ->
            total += runCatching { Res.readBytes(path).size.toLong() }.getOrDefault(0L)
        }
        total.also { cachedMapsSize = it }
    }

    private var cachedMapsSize: Long? = null

    /** Путь к SVG этажа — общий для загрузки и подсчёта объёма. */
    private fun floorPath(campusId: String, floor: Int, oldMp1: Boolean): String = when (campusId) {
        "s-20" -> "files/maps/s-20/floor_$floor.svg"
        "v-78" -> "files/maps/v-78/floor_$floor.svg"
        "v-86" -> "files/maps/v-86/floor_$floor.svg"
        "mp-1" -> if (oldMp1) {
            if (floor == -1) "files/maps/mp-1/floor_m1.svg" else "files/maps/mp-1/floor_$floor.svg"
        } else {
            if (floor == -1) "files/maps/mp-1-new/floor_0.svg"
            else "files/maps/mp-1-new/floor_$floor.svg"
        }
        else -> ""
    }

    private fun guessCampusAndFloor(name: String): Triple<String, String, Int> {
        val trimmed = name.trim()
        // Check V-78 style: letter + hyphen + digits, e.g. "Г-302", "А-438", "В-201"
        val v78Match = Regex("^[А-ЯA-Z]-([0-9])").find(trimmed)
        if (v78Match != null) {
            val floor = v78Match.groupValues[1].toIntOrNull() ?: 1
            return Triple("v-78", "Вернадского 78", floor)
        }

        // Check 3-digit room, e.g. "127", "234", "315" -> likely S-20 or MP-1
        val numMatch = Regex("^([0-9])").find(trimmed)
        if (numMatch != null) {
            val floor = numMatch.groupValues[1].toIntOrNull() ?: 1
            return Triple("s-20", "Стромынка 20", floor)
        }

        return Triple("s-20", "Стромынка 20", 1)
    }
}
