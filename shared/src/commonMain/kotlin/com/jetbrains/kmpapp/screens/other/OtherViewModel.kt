package com.jetbrains.kmpapp.screens.other

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.TaskRepository
import com.jetbrains.kmpapp.data.DebugConfig
import com.jetbrains.kmpapp.data.analytics.AppAnalytics
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.ScheduleTargetType
import com.jetbrains.kmpapp.data.model.StorageStats
import com.jetbrains.kmpapp.data.model.ThemeMode
import com.jetbrains.kmpapp.theme.ThemeOverlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import com.jetbrains.kmpapp.data.update.AppUpdateChecker
import com.jetbrains.kmpapp.data.update.UpdateCheckResult
import com.jetbrains.kmpapp.screens.components.AppTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

enum class TargetSortOrder(val displayName: String) {
    TITLE_ASC("По названию (А → Я / 0 → 9)"),
    TITLE_DESC("По названию (Я → А / 9 → 0)"),
    RECENT("Сначала новые"),
    OLDEST("Сначала старые")
}

enum class OtherSubScreen(val depth: Int) {
    ROOT(0),
    MANAGE_SCHEDULES(1),
    SETTINGS(1),
    DATA_AND_CACHE(2),
    DOCK_SETTINGS(2),
    TASK_SETTINGS(2),
    RESOURCES(1),
    ABOUT(1),
    DEBUG_SETTINGS(2),
    EXPERIMENTAL_SETTINGS(3),
    ICON_PICKER(2),
    // Сервисы, открытые через блок «Сервисы» на главной «Другого».
    SERVICE_ROOMS(1),
    SERVICE_TASKS(1),
    SERVICE_MAP(1),
    SERVICE_NOTES(1),
    SERVICE_COMPARE(1)
}

/** Родитель подстраницы для послойной навигации (null = корень). */
private val SUB_SCREEN_PARENT = mapOf(
    OtherSubScreen.MANAGE_SCHEDULES to OtherSubScreen.ROOT,
    OtherSubScreen.SETTINGS to OtherSubScreen.ROOT,
    OtherSubScreen.DATA_AND_CACHE to OtherSubScreen.SETTINGS,
    OtherSubScreen.DOCK_SETTINGS to OtherSubScreen.SETTINGS,
    OtherSubScreen.TASK_SETTINGS to OtherSubScreen.SETTINGS,
    OtherSubScreen.RESOURCES to OtherSubScreen.ROOT,
    OtherSubScreen.ABOUT to OtherSubScreen.ROOT,
    OtherSubScreen.DEBUG_SETTINGS to OtherSubScreen.ABOUT,
    OtherSubScreen.EXPERIMENTAL_SETTINGS to OtherSubScreen.DEBUG_SETTINGS,
    OtherSubScreen.ICON_PICKER to OtherSubScreen.SETTINGS,
    OtherSubScreen.SERVICE_ROOMS to OtherSubScreen.ROOT,
    OtherSubScreen.SERVICE_TASKS to OtherSubScreen.ROOT,
    OtherSubScreen.SERVICE_MAP to OtherSubScreen.ROOT,
    OtherSubScreen.SERVICE_NOTES to OtherSubScreen.ROOT,
    OtherSubScreen.SERVICE_COMPARE to OtherSubScreen.ROOT
)

fun OtherSubScreen.parent(): OtherSubScreen? =
    if (this == OtherSubScreen.ROOT) null else SUB_SCREEN_PARENT[this]

/** Сервисная подстраница для вкладки дока (null — не сервис). */
fun AppTab.toServiceSubScreen(): OtherSubScreen? = when (this) {
    AppTab.FREE_ROOMS -> OtherSubScreen.SERVICE_ROOMS
    AppTab.TASKS -> OtherSubScreen.SERVICE_TASKS
    AppTab.MAP -> OtherSubScreen.SERVICE_MAP
    AppTab.NOTES -> OtherSubScreen.SERVICE_NOTES
    AppTab.COMPARE -> OtherSubScreen.SERVICE_COMPARE
    else -> null
}

/** Открыт ли в «Другом» сервис (для стрелки «назад» в доке). */
val OtherSubScreen.isServiceScreen: Boolean
    get() = this in setOf(
        OtherSubScreen.SERVICE_ROOMS,
        OtherSubScreen.SERVICE_TASKS,
        OtherSubScreen.SERVICE_MAP,
        OtherSubScreen.SERVICE_NOTES,
        OtherSubScreen.SERVICE_COMPARE
    )

class OtherViewModel(
    private val repository: ScheduleRepository,
    private val updateChecker: AppUpdateChecker,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val savedTargets: StateFlow<List<ScheduleTarget>> = repository.savedTargets
    val selectedTarget: StateFlow<ScheduleTarget?> = repository.selectedTarget
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val showEmptyLessons: StateFlow<Boolean> = repository.showEmptyLessons
    val showLessonProgress: StateFlow<Boolean> = repository.showLessonProgress
    val autoScrollToCurrentLesson: StateFlow<Boolean> = repository.autoScrollToCurrentLesson
    val showAbbreviatedNames: StateFlow<Boolean> = repository.showAbbreviatedNames
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
    val themeOverlay: StateFlow<ThemeOverlay> = repository.themeOverlay
    val isSakuraTheme: StateFlow<Boolean> = repository.isSakuraTheme
    val isCyberpunkTheme: StateFlow<Boolean> = repository.isCyberpunkTheme
    val isMatrixTheme: StateFlow<Boolean> = repository.isMatrixTheme
    val cheatsAgreed: StateFlow<Boolean?> = repository.cheatsAgreed
    val cheatsBlocked: StateFlow<Boolean> = repository.cheatsBlocked
    val dockTabs: StateFlow<List<AppTab>> = repository.dockTabs
    val betaChannel: StateFlow<Boolean> = repository.betaChannel
    val analyticsEnabled: StateFlow<Boolean> = repository.analyticsEnabled
    val appIcon: StateFlow<String> = repository.appIcon
    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled
    val notifyMinutesBefore: StateFlow<Int> = repository.notifyMinutesBefore
    val askBeforeNoteDelete: StateFlow<Boolean> = repository.askBeforeNoteDelete

    fun setShowLessonProgress(enabled: Boolean) {
        repository.setShowLessonProgress(enabled)
    }

    fun setAutoScrollToCurrentLesson(enabled: Boolean) {
        repository.setAutoScrollToCurrentLesson(enabled)
    }

    fun setThemeOverlay(overlay: ThemeOverlay) {
        repository.setThemeOverlay(overlay)
    }

    fun setSakuraTheme(enabled: Boolean) {
        repository.setSakuraTheme(enabled)
    }

    fun setCyberpunkTheme(enabled: Boolean) {
        repository.setCyberpunkTheme(enabled)
    }

    fun setMatrixTheme(enabled: Boolean) = repository.setMatrixTheme(enabled)
    fun setCheatsAgreed(agreed: Boolean?) = repository.setCheatsAgreed(agreed)
    fun setCheatsBlocked(blocked: Boolean) = repository.setCheatsBlocked(blocked)
    fun setAnalyticsEnabled(enabled: Boolean) = repository.setAnalyticsEnabled(enabled)
    fun setAppIcon(name: String) = repository.setAppIcon(name)
    fun setNotificationsEnabled(enabled: Boolean) = repository.setNotificationsEnabled(enabled)
    fun setNotifyMinutesBefore(minutes: Int) = repository.setNotifyMinutesBefore(minutes)
    fun setAskBeforeNoteDelete(ask: Boolean) = repository.setAskBeforeNoteDelete(ask)

    fun setDockTabs(tabs: List<AppTab>) {
        repository.setDockTabs(tabs)
    }

    private val _activeSubScreen = MutableStateFlow(OtherSubScreen.ROOT)
    val activeSubScreen: StateFlow<OtherSubScreen> = _activeSubScreen.asStateFlow()

    fun navigateToSubScreen(subScreen: OtherSubScreen) {
        _activeSubScreen.value = subScreen
        if (subScreen != OtherSubScreen.ROOT) {
            AppAnalytics.logEvent("screen_view", mapOf("screen" to subScreen.name))
        }
    }

    fun resetToRoot() {
        _activeSubScreen.value = OtherSubScreen.ROOT
    }

    private val _storageStats = MutableStateFlow(repository.getStorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    fun refreshStorageStats() {
        _storageStats.value = repository.getStorageStats()
    }

    private val _contributors = MutableStateFlow<List<com.jetbrains.kmpapp.data.model.GitHubContributor>>(
        listOf(
            com.jetbrains.kmpapp.data.model.GitHubContributor(
                login = "l1ratch",
                htmlUrl = "https://github.com/l1ratch",
                avatarUrl = "https://avatars.githubusercontent.com/u/103525164?v=4",
                contributions = 14,
                role = "Создатель и ведущий разработчик"
            ),
            com.jetbrains.kmpapp.data.model.GitHubContributor(
                login = "prosto-max",
                htmlUrl = "https://github.com/prosto-max",
                avatarUrl = "https://avatars.githubusercontent.com/u/151039381?v=4",
                contributions = 5,
                role = "Соавтор и разработчик"
            )
        )
    )
    val contributors: StateFlow<List<com.jetbrains.kmpapp.data.model.GitHubContributor>> = _contributors.asStateFlow()

    private val _isLoadingContributors = MutableStateFlow(false)
    val isLoadingContributors: StateFlow<Boolean> = _isLoadingContributors.asStateFlow()

    fun loadContributors() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _isLoadingContributors.value = true
                val fetched = updateChecker.fetchContributors(forceRefresh = true)
                val l1ratchFromApi = fetched.find { it.login.equals("l1ratch", ignoreCase = true) }
                val prostoMaxFromApi = fetched.find { it.login.equals("prosto-max", ignoreCase = true) }
                val staticLead = com.jetbrains.kmpapp.data.model.GitHubContributor(
                    login = "l1ratch",
                    htmlUrl = "https://github.com/l1ratch",
                    avatarUrl = l1ratchFromApi?.avatarUrl ?: "https://avatars.githubusercontent.com/u/103525164?v=4",
                    contributions = l1ratchFromApi?.contributions ?: 14,
                    role = "Создатель и ведущий разработчик"
                )
                val coAuthor = com.jetbrains.kmpapp.data.model.GitHubContributor(
                    login = "prosto-max",
                    htmlUrl = "https://github.com/prosto-max",
                    avatarUrl = prostoMaxFromApi?.avatarUrl ?: "https://github.com/prosto-max.png",
                    contributions = prostoMaxFromApi?.contributions ?: 5,
                    role = "Соавтор и разработчик"
                )
                val otherContributors = fetched.filterNot {
                    it.login.equals("l1ratch", ignoreCase = true) || it.login.equals("prosto-max", ignoreCase = true)
                }
                _contributors.value = listOf(staticLead, coAuthor) + otherContributors
            } catch (t: Throwable) {
                println("Failed to load contributors: ${t.message}")
            } finally {
                _isLoadingContributors.value = false
            }
        }
    }

    private val _updateResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateResult: StateFlow<UpdateCheckResult?> = _updateResult.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateStatusMessage = MutableStateFlow<String?>(null)
    val updateStatusMessage: StateFlow<String?> = _updateStatusMessage.asStateFlow()

    fun setBetaChannel(enabled: Boolean) {
        repository.setBetaChannel(enabled)
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _isCheckingUpdate.value = true
                _updateStatusMessage.value = null
                val result = updateChecker.checkForUpdates(betaChannel.value)
                _updateResult.value = result
                _isCheckingUpdate.value = false
                if (result != null && !result.hasUpdate) {
                    _updateStatusMessage.value = "У вас установлена последняя версия (${result.currentVersion})"
                } else if (result == null) {
                    _updateStatusMessage.value = "Не удалось проверить обновления"
                }
            } catch (t: Throwable) {
                println("checkForUpdates caught throwable: ${t.message}")
                _isCheckingUpdate.value = false
                _updateStatusMessage.value = null
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateResult.value = null
        _updateStatusMessage.value = null
    }

    // Search, filter, and sort state for ManageSchedulesScreen
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<ScheduleTargetType?>(null)
    val filterType: StateFlow<ScheduleTargetType?> = _filterType.asStateFlow()

    private val _sortOrder = MutableStateFlow(TargetSortOrder.TITLE_ASC)
    val sortOrder: StateFlow<TargetSortOrder> = _sortOrder.asStateFlow()

    val filteredSavedTargets: StateFlow<List<ScheduleTarget>> = combine(
        repository.savedTargets,
        _searchQuery,
        _filterType,
        _sortOrder
    ) { list, query, filter, sort ->
        var result = list
        if (filter != null) {
            result = result.filter { it.type == filter }
        }
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            result = result.filter {
                it.targetTitle.contains(trimmed, ignoreCase = true) ||
                it.fullTitle.contains(trimmed, ignoreCase = true)
            }
        }
        when (sort) {
            TargetSortOrder.TITLE_ASC -> result.sortedBy { it.targetTitle.lowercase() }
            TargetSortOrder.TITLE_DESC -> result.sortedByDescending { it.targetTitle.lowercase() }
            TargetSortOrder.RECENT -> result
            TargetSortOrder.OLDEST -> result.reversed()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: ScheduleTargetType?) {
        _filterType.value = type
    }

    fun setSortOrder(order: TargetSortOrder) {
        _sortOrder.value = order
    }

    fun toggleSortDirection() {
        _sortOrder.value = when (_sortOrder.value) {
            TargetSortOrder.TITLE_ASC -> TargetSortOrder.TITLE_DESC
            TargetSortOrder.TITLE_DESC -> TargetSortOrder.TITLE_ASC
            TargetSortOrder.RECENT -> TargetSortOrder.OLDEST
            TargetSortOrder.OLDEST -> TargetSortOrder.RECENT
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }

    fun setShowEmptyLessons(enabled: Boolean) {
        repository.setShowEmptyLessons(enabled)
    }

    fun setShowAbbreviatedNames(enabled: Boolean) {
        repository.setShowAbbreviatedNames(enabled)
    }

    fun selectTarget(target: ScheduleTarget) {
        repository.selectTarget(target)
    }

    fun removeTarget(targetId: Int) {
        repository.removeTarget(targetId)
    }

    fun refreshSchedule() {
        repository.refreshCurrentSchedule()
    }

    fun clearCache() {
        repository.clearCache()
        _storageStats.value = repository.getStorageStats()
    }

    fun resetAllData() {
        taskRepository.clearAllData()
        repository.resetAllData()
        DebugConfig.reset()
        _storageStats.value = repository.getStorageStats()
    }

    suspend fun search(query: String): List<ScheduleTarget> {
        return repository.search(query)
    }

    fun addAndSelectTarget(target: ScheduleTarget) {
        repository.addAndSelectTarget(target)
    }
}
