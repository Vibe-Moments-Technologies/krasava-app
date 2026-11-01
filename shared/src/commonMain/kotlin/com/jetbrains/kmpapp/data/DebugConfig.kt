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
     * Правило: работает ТОЛЬКО на dev/beta/contrib-сборках; в стабильном
     * релизе тумблер недоступен и флаг принудительно false — офидийный фид
     * защищён от подмены.
     */
    private val _isTestVersionFeed = MutableStateFlow(false)
    val isTestVersionFeed: StateFlow<Boolean> = _isTestVersionFeed.asStateFlow()

    // Стабильный релиз: тестовый фид недопустим (правило владельца).
    private val testFeedAllowed: Boolean =
        com.jetbrains.kmpapp.data.model.AppVersion.BUILD_CHANNEL !in setOf("stable", "rc")

    fun setTestVersionFeed(enabled: Boolean) {
        _isTestVersionFeed.value = enabled && testFeedAllowed
    }

    fun reset() {
        _isOfflineSimulated.value = false
        _isTestVersionFeed.value = false
    }
}
