package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.storage.KeyValueStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Кеш аватарок ВК ([VkAvatarLoader]).
 *
 * Регресс, из-за которого затевался кеш: сопоставление «страница → URL
 * аватарки» жило только в памяти, поэтому при каждом запуске приложение
 * заново скрапило все страницы ВК, и аватарки появлялись с задержкой.
 * Теперь URL живёт на диске, а неудачи — только в памяти (чтобы временный
 * блок ВК не залип навсегда).
 *
 * Сеть подменяется лямбдой загрузки HTML: считаем запросы и отдаём
 * заранее заданную разметку.
 */
class VkAvatarLoaderTest {

    private class FakeStore : KeyValueStore {
        val saved = mutableMapOf<String, String>()
        override fun saveString(key: String, value: String) { saved[key] = value }
        override fun getString(key: String): String? = saved[key]
        override fun remove(key: String) { saved.remove(key) }
        override fun clearAll() { saved.clear() }
    }

    /** Хранилище, которое «пережило перезапуск» — копия сохранённого. */
    private fun restarted(store: FakeStore) = FakeStore().also { it.saved.putAll(store.saved) }

    private fun html(avatarUrl: String) =
        """<html><head><meta property="og:image" content="$avatarUrl"></head></html>"""

    private fun htmlWithoutAvatar() = "<html><head><title>ВК</title></head></html>"

    private fun loader(
        store: FakeStore,
        now: () -> Long = { 0L },
        fetch: suspend (String) -> String?
    ) = VkAvatarLoader(fetchHtml = fetch, store = store, now = now)

    @Test
    fun resolvesAvatarFromPage() = runBlocking {
        var requests = 0
        val loader = loader(FakeStore()) { requests++; html("https://sun9.vk/img.jpg") }

        assertEquals("https://sun9.vk/img.jpg", loader.resolveAvatar("https://vk.ru/sumirea"))
        assertEquals(1, requests)
    }

    @Test
    fun secondCallInSameSessionUsesMemoryCache() = runBlocking {
        var requests = 0
        val loader = loader(FakeStore()) { requests++; html("https://sun9.vk/a.jpg") }

        loader.resolveAvatar("https://vk.ru/sumirea")
        loader.resolveAvatar("https://vk.ru/sumirea")
        loader.resolveAvatar("https://vk.ru/sumirea")

        assertEquals(1, requests, "в пределах сессии страница запрашивается один раз")
    }

    /** Главный смысл правки: перезапуск не должен заново скрапить страницы. */
    @Test
    fun survivesRestartWithoutNetworkRequest() = runBlocking {
        val store = FakeStore()
        var requests = 0
        loader(store) { requests++; html("https://sun9.vk/persist.jpg") }
            .resolveAvatar("https://vk.ru/sumirea")
        assertEquals(1, requests)

        // «Перезапуск»: новый экземпляр поверх сохранённых данных.
        var afterRestart = 0
        val avatar = loader(restarted(store)) {
            afterRestart++
            html("https://sun9.vk/other.jpg")
        }.resolveAvatar("https://vk.ru/sumirea")

        assertEquals("https://sun9.vk/persist.jpg", avatar)
        assertEquals(0, afterRestart, "после перезапуска сеть не дёргается вовсе")
    }

    /** Запись старше TTL освежается: аватар сообщества мог смениться. */
    @Test
    fun staleEntryIsRefetched() = runBlocking {
        val store = FakeStore()
        var clock = 0L

        var requests = 0
        loader(store, now = { clock }) { requests++; html("https://sun9.vk/old.jpg") }
            .resolveAvatar("https://vk.ru/sumirea")
        assertEquals(1, requests)

        // Две недели + минута: срок истёк.
        clock = 15L * 24 * 60 * 60 * 1000
        var fresh = 0
        val avatar = loader(restarted(store), now = { clock }) {
            fresh++
            html("https://sun9.vk/new.jpg")
        }.resolveAvatar("https://vk.ru/sumirea")

        assertEquals("https://sun9.vk/new.jpg", avatar)
        assertEquals(1, fresh, "просроченная запись перезапрашивается")
    }

    /** Неудача не сохраняется на диск: временный блок ВК не должен залипнуть. */
    @Test
    fun failureIsNotPersistedOnDisk() = runBlocking {
        val store = FakeStore()
        assertNull(loader(store) { throw RuntimeException("captcha") }
            .resolveAvatar("https://vk.ru/sumirea"))
        assertTrue(store.saved.isEmpty(), "неудача не пишется на диск")

        // Следующий запуск должен попробовать снова.
        var requests = 0
        assertEquals(
            "https://sun9.vk/ok.jpg",
            loader(restarted(store)) { requests++; html("https://sun9.vk/ok.jpg") }
                .resolveAvatar("https://vk.ru/sumirea")
        )
        assertEquals(1, requests)
    }

    /** Но в пределах сессии неудача не превращается в шквал запросов. */
    @Test
    fun failureIsCachedInMemoryForSession() = runBlocking {
        var requests = 0
        val loader = loader(FakeStore()) { requests++; throw RuntimeException("no network") }

        assertNull(loader.resolveAvatar("https://vk.ru/sumirea"))
        assertNull(loader.resolveAvatar("https://vk.ru/sumirea"))
        assertEquals(1, requests, "в сессии неудачу не повторяем")
    }

    @Test
    fun pageWithoutAvatarReturnsNull() = runBlocking {
        assertNull(loader(FakeStore()) { htmlWithoutAvatar() }
            .resolveAvatar("https://vk.ru/sumirea"))
    }

    @Test
    fun blankUrlIsIgnored() = runBlocking {
        var requests = 0
        val loader = loader(FakeStore()) { requests++; html("https://sun9.vk/x.jpg") }

        assertNull(loader.resolveAvatar("   "))
        assertEquals(0, requests)
    }

    @Test
    fun clearWipesDiskAndMemory() = runBlocking {
        val store = FakeStore()
        var requests = 0
        val loader = loader(store) { requests++; html("https://sun9.vk/a.jpg") }

        loader.resolveAvatar("https://vk.ru/sumirea")
        assertEquals(1, requests)

        loader.clear()

        loader.resolveAvatar("https://vk.ru/sumirea")
        assertEquals(2, requests, "после clear данные берутся заново")
    }

    @Test
    fun corruptDiskEntryDoesNotBreakLoading() = runBlocking {
        val store = FakeStore()
        store.saveString("krasava_vk_avatar_cache", "{not json at all")
        var requests = 0
        val avatar = loader(store) { requests++; html("https://sun9.vk/ok.jpg") }
            .resolveAvatar("https://vk.ru/sumirea")

        assertEquals("https://sun9.vk/ok.jpg", avatar)
        assertEquals(1, requests)
    }

    @Test
    fun differentPagesAreCachedIndependently() = runBlocking {
        val store = FakeStore()
        var requests = 0
        val loader = loader(store) {
            requests++
            html("https://sun9.vk/$requests.jpg")
        }

        assertEquals("https://sun9.vk/1.jpg", loader.resolveAvatar("https://vk.ru/sumirea"))
        assertEquals("https://sun9.vk/2.jpg", loader.resolveAvatar("https://vk.ru/apriori"))
        assertEquals(2, requests)

        // Оба лежат на диске — после перезапуска ни одного запроса.
        var after = 0
        val loader2 = loader(restarted(store)) { after++; html("https://sun9.vk/z.jpg") }
        assertEquals("https://sun9.vk/1.jpg", loader2.resolveAvatar("https://vk.ru/sumirea"))
        assertEquals("https://sun9.vk/2.jpg", loader2.resolveAvatar("https://vk.ru/apriori"))
        assertEquals(0, after)
    }

    /** Неудача по одной странице не мешает закешировать другую. */
    @Test
    fun failureOnOnePageDoesNotBlockAnother() = runBlocking {
        val store = FakeStore()
        val loader = loader(store) { page ->
            if (page.contains("sumirea")) throw RuntimeException("blocked")
            html("https://sun9.vk/ok.jpg")
        }

        assertNull(loader.resolveAvatar("https://vk.ru/sumirea"))
        assertEquals("https://sun9.vk/ok.jpg", loader.resolveAvatar("https://vk.ru/apriori"))
    }
}