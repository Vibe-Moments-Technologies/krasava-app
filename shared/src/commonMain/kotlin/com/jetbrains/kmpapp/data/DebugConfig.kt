package com.jetbrains.kmpapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DebugConfig {
    private val _isOfflineSimulated = MutableStateFlow(false)
    val isOfflineSimulated: StateFlow<Boolean> = _isOfflineSimulated.asStateFlow()

    fun setOfflineSimulated(enabled: Boolean) {
        _isOfflineSimulated.value = enabled
    }

    private val _isMapCoordinatePlaneEnabled = MutableStateFlow(false)
    val isMapCoordinatePlaneEnabled: StateFlow<Boolean> = _isMapCoordinatePlaneEnabled.asStateFlow()

    fun setMapCoordinatePlaneEnabled(enabled: Boolean) {
        _isMapCoordinatePlaneEnabled.value = enabled
    }

    /**
     * Тестовый фид версионирования (репорт владельца): проверка обновлений
     * читает version-test.json на gh-pages вместо официального version.json.
     *
     * Персистентен (репорт: «после перезапуска сбрасывается»): флаг
     * хранится в PlatformStorage, при старте читается. Правило владельца
     * не меняется: работает ТОЛЬКО на dev/beta/contrib-сборках; в
     * стабильном релизе (stable/rc) чтение гасит флаг и запись
     * игнорируется — официальный фид защищён от подмены.
     */
    private const val KEY_TEST_VERSION_FEED = "krasava_debug_test_version_feed"

    // Стабильный релиз: тестовый фид недопустим (правило владельца).
    private val testFeedAllowed: Boolean =
        com.jetbrains.kmpapp.data.model.AppVersion.BUILD_CHANNEL !in setOf("stable", "rc")

    private val storage by lazy {
        com.jetbrains.kmpapp.data.storage.PlatformStorage()
    }

    private val _isTestVersionFeed = MutableStateFlow(
        // Персистентность: стартовое значение — из хранилища (и только
        // на каналах, где тестовый фид разрешён).
        testFeedAllowed &&
            runCatching { storage.getString(KEY_TEST_VERSION_FEED) == "1" }.getOrDefault(false)
    )
    val isTestVersionFeed: StateFlow<Boolean> = _isTestVersionFeed.asStateFlow()

    fun setTestVersionFeed(enabled: Boolean) {
        val allowed = enabled && testFeedAllowed
        _isTestVersionFeed.value = allowed
        if (testFeedAllowed) {
            runCatching { storage.saveString(KEY_TEST_VERSION_FEED, if (allowed) "1" else "0") }
        }
    }

    fun reset() {
        _isOfflineSimulated.value = false
        _isTestVersionFeed.value = false
        runCatching { storage.remove(KEY_TEST_VERSION_FEED) }
    }

    /**
     * Новая сьёмка Малой Пироговской (репорт владельца): карты mp-1-new
     * вместо старых mp-1. По умолчанию — СТАРАЯ версия (решение владельца:
     * «старую не убирай, по умолчанию — старая, новую включать в отладке»).
     * Тумблер в «Отладке и экспериментах», персистентен (как тестовый фид).
     */
    private const val KEY_NEW_MP1_MAP = "krasava_debug_new_mp1_map"

    private val _isNewMp1MapEnabled = MutableStateFlow(
        runCatching { storage.getString(KEY_NEW_MP1_MAP) == "1" }.getOrDefault(false)
    )
    val isNewMp1MapEnabled: StateFlow<Boolean> = _isNewMp1MapEnabled.asStateFlow()

    fun setNewMp1MapEnabled(enabled: Boolean) {
        _isNewMp1MapEnabled.value = enabled
        runCatching { storage.saveString(KEY_NEW_MP1_MAP, if (enabled) "1" else "0") }
    }
}
