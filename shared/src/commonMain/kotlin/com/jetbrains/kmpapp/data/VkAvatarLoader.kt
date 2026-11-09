package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.storage.KeyValueStore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Достаёт аватар сообщества ВКонтакте из HTML страницы (meta og:image).
 *
 * Кеширование в три уровня, потому что «не кешируется» выглядело как
 * долгие аватарки при каждом запуске:
 *
 *  1. **Память** — мгновенный ответ в пределах жизни процесса.
 *  2. **Диск** ([KeyValueStore]) — найденный URL переживает перезапуск, так
 *     что при следующем старте приложение НЕ ходит в ВК за HTML вовсе:
 *     сразу отдаёт сохранённый URL, а саму картинку берёт из дискового кеша
 *     Coil. Раньше карта жила только в памяти и 13 страниц скрапились заново
 *     при каждом запуске.
 *  3. **TTL** ([TTL_MS]) — раз в две недели ссылка освежается: аватар
 *     сообщества может смениться, и «навсегда» показывало бы старую.
 *
 * Неудачи (капча, блок, нет сети) кешируются ТОЛЬКО в памяти: иначе один
 * временный блок ВК залип бы на диске навсегда, и пользователь до сброса
 * данных видел бы заглушки. В пределах сессии повторных запросов нет.
 */
class VkAvatarLoader(
    /** Как получить HTML страницы. Вынесено в лямбду, чтобы тесты не тянули сеть. */
    private val fetchHtml: suspend (pageUrl: String) -> String?,
    private val store: KeyValueStore,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {

    constructor(
        client: HttpClient,
        store: KeyValueStore,
        now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
    ) : this(fetchHtml = { pageUrl -> fetchPage(client, pageUrl) }, store = store, now = now)

    private val ogImage = Regex(
        """<meta(?=[^>]*?["']og:image["'])[^>]*?\bcontent\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Сеть может дёргаться из нескольких карточек разом — карту и файл сериализуем. */
    private val mutex = Mutex()

    /** Быстрый путь: и успехи, и неудачи текущей сессии. */
    private val memory = mutableMapOf<String, String?>()

    /** Успешные URL, переживающие перезапуск (загружается лениво один раз). */
    private var persisted: MutableMap<String, CachedAvatar>? = null

    /**
     * @return URL картинки-аватара либо null, если достать не удалось
     * (UI покажет символьную заглушку).
     */
    suspend fun resolveAvatar(pageUrl: String): String? {
        val key = pageUrl.trim()
        if (key.isEmpty()) return null

        // containsKey, а не memory[key]?.let: закешированная НЕУДАЧА — это
        // ключ со значением null, и обращение по индексу её не отличит от
        // отсутствия записи (тогда в сессии долбили бы сеть каждый раз).
        mutex.withLock {
            if (memory.containsKey(key)) return memory[key]
        }

        // Диск: свежая запись отдаётся без единого сетевого запроса.
        val cached = mutex.withLock { persistedMap()[key] }
        if (cached != null && now() - cached.savedAt < TTL_MS) {
            mutex.withLock { memory[key] = cached.url }
            return cached.url
        }

        val avatar = fetch(key)

        mutex.withLock {
            // null в память кладём (в этой сессии не долбим), на диск — нет.
            memory[key] = avatar
            if (avatar != null) {
                val map = persistedMap()
                map[key] = CachedAvatar(url = avatar, savedAt = now())
                savePersisted(map)
            }
        }
        return avatar
    }

    private suspend fun fetch(url: String): String? = try {
        fetchHtml(url)?.let { html -> ogImage.find(html)?.groupValues?.get(1) }
    } catch (_: Throwable) {
        null
    }

    private fun persistedMap(): MutableMap<String, CachedAvatar> =
        persisted ?: loadPersisted().also { persisted = it }

    private fun loadPersisted(): MutableMap<String, CachedAvatar> = try {
        val raw = store.getString(KEY_CACHE)
        if (raw.isNullOrBlank()) {
            mutableMapOf()
        } else {
            json.decodeFromString<Map<String, CachedAvatar>>(raw).toMutableMap()
        }
    } catch (_: Throwable) {
        // Битая запись не должна ломать экран: начинаем кеш заново.
        mutableMapOf()
    }

    private fun savePersisted(map: Map<String, CachedAvatar>) {
        try {
            store.saveString(KEY_CACHE, json.encodeToString(map.toMap()))
        } catch (_: Throwable) {
            // Не смогли сохранить — работаем из памяти, это не критично.
        }
    }

    /**
     * Сбросить кеш аватарок (например, при полном сбросе данных).
     * Сейчас вызывается только явно; записи сами истекают по TTL.
     */
    fun clear() {
        memory.clear()
        persisted = null
        try {
            store.remove(KEY_CACHE)
        } catch (_: Throwable) {
        }
    }

    @Serializable
    private data class CachedAvatar(
        val url: String,
        val savedAt: Long
    )

    private companion object {
        /** Хранилище: один JSON-объект «страница → аватар», пар десяток. */
        const val KEY_CACHE = "krasava_vk_avatar_cache"

        /** Две недели: аватар сообщества может смениться, но не часто. */
        const val TTL_MS = 14L * 24 * 60 * 60 * 1000
    }
}

/** Забирает HTML страницы ВК, прикидываясь обычным браузером. */
private suspend fun fetchPage(client: HttpClient, url: String): String? = try {
    client.get(url) {
        header(
            HttpHeaders.UserAgent,
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        )
        header(HttpHeaders.Accept, "text/html")
    }.bodyAsText()
} catch (_: Throwable) {
    null
}