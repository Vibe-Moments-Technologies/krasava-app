package com.jetbrains.kmpapp.data

import com.jetbrains.kmpapp.data.api.MireaScheduleApi
import com.jetbrains.kmpapp.data.model.CalendarExpandState
import com.jetbrains.kmpapp.data.model.DateUtils
import com.jetbrains.kmpapp.data.model.Lesson
import com.jetbrains.kmpapp.data.model.LessonColor
import com.jetbrains.kmpapp.data.model.LessonType
import com.jetbrains.kmpapp.data.model.LessonDiffItem
import com.jetbrains.kmpapp.data.model.LessonDiffType
import com.jetbrains.kmpapp.data.model.ScheduleDiff
import com.jetbrains.kmpapp.data.model.ScheduleTarget
import com.jetbrains.kmpapp.data.model.ThemeMode
import com.jetbrains.kmpapp.data.model.UserLesson
import com.jetbrains.kmpapp.data.model.UserLessonDeleteScope
import com.jetbrains.kmpapp.data.model.orderedForDisplay
import com.jetbrains.kmpapp.data.network.detectVpnActive
import com.jetbrains.kmpapp.data.parser.MireaICalParser
import com.jetbrains.kmpapp.data.storage.ScheduleStorage
import com.jetbrains.kmpapp.theme.CyberpunkVariant
import com.jetbrains.kmpapp.theme.ThemeOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.jetbrains.kmpapp.data.notifications.NotificationsManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.ktor.utils.io.errors.IOException
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Clock

class ScheduleRepository(
    private val api: MireaScheduleApi,
    private val storage: ScheduleStorage,
    private val powerManager: com.jetbrains.kmpapp.data.power.PlatformPowerManager,
    private val lessonNotesStorage: com.jetbrains.kmpapp.data.storage.LessonNotesStorage
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val notificationRescheduleMutex = Mutex()

    val isLowPowerMode: StateFlow<Boolean> = powerManager.isLowPowerMode
    val savedTargets: StateFlow<List<ScheduleTarget>> = storage.savedTargets
    val selectedTarget: StateFlow<ScheduleTarget?> = storage.selectedTarget
    // Единая точка скрытия ДОП-пар: отсюда читают экран расписания,
    // сравнение расписаний и все производные StateFlow.
    val cachedLessons: StateFlow<Map<Int, List<Lesson>>> = combine(
        storage.cachedLessons,
        storage.hideAdditionalLessons
    ) { cache, hideAdditional ->
        if (!hideAdditional) cache
        else cache.mapValues { (_, lessons) -> lessons.filter { it.lessonType != LessonType.ADDITIONAL } }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())
    val showEmptyLessons: StateFlow<Boolean> = storage.showEmptyLessons
    val themeMode: StateFlow<ThemeMode> = storage.themeMode
    val dockTabs: StateFlow<List<com.jetbrains.kmpapp.screens.components.AppTab>> = storage.dockTabs
    val dockHidden: StateFlow<Boolean> = storage.dockHidden
    val notePages: StateFlow<List<com.jetbrains.kmpapp.data.model.NotePage>> = storage.notePages

    fun updateNotePages(pages: List<com.jetbrains.kmpapp.data.model.NotePage>) {
        storage.saveNotePages(pages)
    }

    fun setDockTabs(tabs: List<com.jetbrains.kmpapp.screens.components.AppTab>) {
        storage.setDockTabs(tabs)
    }

    fun setDockHidden(hidden: Boolean) {
        storage.setDockHidden(hidden)
    }

    private val _activeDiff = MutableStateFlow<ScheduleDiff?>(null)
    val activeDiff: StateFlow<ScheduleDiff?> = _activeDiff.asStateFlow()

    fun dismissDiff() {
        _activeDiff.value = null
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _refreshStatus = MutableStateFlow<com.jetbrains.kmpapp.data.model.RefreshStatus?>(null)
    val refreshStatus: StateFlow<com.jetbrains.kmpapp.data.model.RefreshStatus?> = _refreshStatus.asStateFlow()

    fun clearRefreshStatus() {
        _refreshStatus.value = null
    }

    fun getStorageStats(): com.jetbrains.kmpapp.data.model.StorageStats = storage.getStorageStats()

    fun setShowEmptyLessons(enabled: Boolean) {
        storage.setShowEmptyLessons(enabled)
    }

    val showAbbreviatedNames: StateFlow<Boolean> = storage.showAbbreviatedNames

    fun setShowAbbreviatedNames(enabled: Boolean) {
        storage.setShowAbbreviatedNames(enabled)
    }

    val showLessonProgress: StateFlow<Boolean> = storage.showLessonProgress

    fun setShowLessonProgress(enabled: Boolean) {
        storage.setShowLessonProgress(enabled)
    }

    val showEmptyLessonProgress: StateFlow<Boolean> = storage.showEmptyLessonProgress

    fun setShowEmptyLessonProgress(enabled: Boolean) {
        storage.setShowEmptyLessonProgress(enabled)
    }

    val showBreakProgress: StateFlow<Boolean> = storage.showBreakProgress

    fun setShowBreakProgress(enabled: Boolean) {
        storage.setShowBreakProgress(enabled)
    }

    val calendarCollapsed: StateFlow<Boolean> = storage.calendarCollapsed

    fun setCalendarCollapsed(collapsed: Boolean) {
        storage.setCalendarCollapsed(collapsed)
    }

    val calendarSwipeCollapse: StateFlow<Boolean> = storage.calendarSwipeCollapse

    /** Ступень раскрытия ленты календаря: неделя → две недели → месяц. */
    val calendarExpandState: StateFlow<CalendarExpandState> = storage.calendarExpandState
    fun setCalendarExpandState(state: CalendarExpandState) =
        storage.setCalendarExpandState(state)

    fun setCalendarSwipeCollapse(enabled: Boolean) {
        storage.setCalendarSwipeCollapse(enabled)
    }

    val hideAdditionalLessons: StateFlow<Boolean> = storage.hideAdditionalLessons

    fun setHideAdditionalLessons(enabled: Boolean) {
        storage.setHideAdditionalLessons(enabled)
    }

    val autoScrollToCurrentLesson: StateFlow<Boolean> = storage.autoScrollToCurrentLesson

    fun setAutoScrollToCurrentLesson(enabled: Boolean) {
        storage.setAutoScrollToCurrentLesson(enabled)
    }

    fun setThemeMode(mode: ThemeMode) {
        storage.setThemeMode(mode)
    }

    val themeOverlay: StateFlow<ThemeOverlay> = storage.themeOverlay
    val isSakuraTheme: StateFlow<Boolean> = storage.isSakuraTheme
    val isKawaiiTheme: StateFlow<Boolean> = storage.isKawaiiTheme
    val isCyberpunkTheme: StateFlow<Boolean> = storage.isCyberpunkTheme
    val isMatrixTheme: StateFlow<Boolean> = storage.isMatrixTheme
    val isHalloweenTheme: StateFlow<Boolean> = storage.isHalloweenTheme
    /** Хэллоуин сейчас активен: сезон без отказа или ручное включение. */
    val isHalloweenActive: StateFlow<Boolean> = storage.isHalloweenActive
    /** Строка Хэллоуина видна в списке тем: сезон идёт или тема включена. */
    val isHalloweenRowVisible: StateFlow<Boolean> = storage.isHalloweenRowVisible
    val unlockedThemes: StateFlow<Set<ThemeOverlay>> = storage.unlockedThemes
    /** Пользовательские цвета типов занятий (только переопределённые). */
    val lessonTypeColors: StateFlow<Map<LessonType, LessonColor>> = storage.lessonTypeColors
    /** Вариант палитры «Киберпанка»: новая (жёлтый/циан) или старая (маджента). */
    val cyberpunkVariant: StateFlow<CyberpunkVariant> = storage.cyberpunkVariant
    val cheatsAgreed: StateFlow<Boolean?> = storage.cheatsAgreed
    val cheatsBlocked: StateFlow<Boolean> = storage.cheatsBlocked
    val diagnosticsEnabled: StateFlow<Boolean> = storage.diagnosticsEnabled
    val appIcon: StateFlow<String> = storage.appIcon
    val notificationsEnabled: StateFlow<Boolean> = storage.notificationsEnabled
    val notifyMinutesBefore: StateFlow<Int> = storage.notifyMinutesBefore
    val notificationsTargetId: StateFlow<Int?> = storage.notificationsTargetId
    val vpnWarningEnabled: StateFlow<Boolean> = storage.vpnWarningEnabled
    val qrButtonEnabled: StateFlow<Boolean> = storage.qrButtonEnabled
    val askBeforeNoteDelete: StateFlow<Boolean> = storage.askBeforeNoteDelete

    val hiddenLessonIds: StateFlow<Set<String>> = combine(
        storage.selectedTarget,
        storage.hiddenLessons
    ) { selected, hidden ->
        selected?.let { storage.hiddenLessonIdsFor(it, hidden) }.orEmpty()
    }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    val hiddenLessonsByTarget: StateFlow<Map<Int, Set<String>>> = combine(
        storage.savedTargets,
        storage.hiddenLessons
    ) { targets, hidden ->
        targets.associate { target -> target.id to storage.hiddenLessonIdsFor(target, hidden) }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    val currentLessons: StateFlow<List<Lesson>> = combine(
        storage.selectedTarget,
        storage.cachedLessons,
        storage.hideAdditionalLessons,
        storage.userLessons
    ) { selected, cache, hideAdditional, userLessons ->
        val remote = if (selected == null) emptyList() else cache[selected.id] ?: emptyList()
        // Пользовательские пары идут ПЕРВЫМИ: при совпадении звонка карточка
        // «Своя пара» должна открываться раньше пары из фида.
        val userOccurrences = selected
            ?.let { storage.userLessonsFor(it, userLessons) }
            ?.let { expandUserLessons(it) }
            .orEmpty()
        val merged = userOccurrences + remote
        if (hideAdditional) merged.filter { it.lessonType != LessonType.ADDITIONAL }
        else merged
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val displayLessonsByTarget: StateFlow<Map<Int, List<Lesson>>> = combine(
        storage.savedTargets,
        storage.cachedLessons,
        storage.hideAdditionalLessons,
        storage.userLessons
    ) { targets, cache, hideAdditional, userLessons ->
        targets.associate { target ->
            val remote = cache[target.id].orEmpty()
            val userOccurrences = expandUserLessons(storage.userLessonsFor(target, userLessons))
            val merged = userOccurrences + remote
            target.id to if (hideAdditional) {
                merged.filter { it.lessonType != LessonType.ADDITIONAL }
            } else {
                merged
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    init {
        // Единая точка перепланирования напоминаний: кэш расписания, выбранная
        // цель, тумблер или минуты — любое изменение (и старт приложения)
        // пересчитывают партию уведомлений. Лишние пересчёты дешёвые: движок
        // начинает с cancelAll.
        scope.launch {
            var scheduledTargetId: Int? = null
            var notificationStateInitialized = false
            // combine принимает максимум 5 потоков — настройки свёрнуты в тройку.
            val notifSettings = combine(
                storage.notificationsEnabled,
                storage.notifyMinutesBefore,
                storage.hideAdditionalLessons
            ) { enabled, minutes, hideAdditional -> Triple(enabled, minutes, hideAdditional) }
            combine(
                displayLessonsByTarget,
                storage.selectedTarget,
                storage.notificationsTargetId,
                notifSettings,
                storage.hiddenLessons
            ) { lessons, activeTarget, notificationTargetId, (enabled, minutes, hideAdditional), hidden ->
                NotificationsPayload(
                    lessons,
                    activeTarget,
                    enabled,
                    minutes,
                    notificationTargetId,
                    hideAdditional,
                    hidden
                )
            }.collect { p ->
                notificationRescheduleMutex.withLock {
                    val target = p.notificationTargetId?.let { id ->
                        p.activeTarget?.takeIf { it.id == id }
                            ?: storage.savedTargets.value.firstOrNull { it.id == id }
                    } ?: p.activeTarget?.also {
                        // Миграция старых установок: раньше отдельной цели не было,
                        // берём активное расписание и фиксируем его явно.
                        storage.setNotificationsTargetId(it.id)
                    }
                    val hiddenIds = target?.let { storage.hiddenLessonIdsFor(it, p.hiddenLessons) }.orEmpty()
                    val targetLessons = target?.let { p.lessons[it.id] }
                        ?.let { list ->
                            val filtered = if (p.hideAdditionalLessons) {
                                list.filter { lesson -> lesson.lessonType != LessonType.ADDITIONAL }
                            } else {
                                list
                            }
                            filtered.orderedForDisplay(hiddenIds)
                                .distinctBy { lesson -> lesson.date to lesson.bellNumber }
                        }
                    when {
                        !p.enabled || target == null -> {
                            NotificationsManager.reschedule(emptyList(), p.minutes) { "" } // снимает всё
                            scheduledTargetId = null
                        }
                        targetLessons != null -> {
                            NotificationsManager.reschedule(targetLessons, p.minutes) { lesson ->
                                // «СДО» — сайт вуза, не аудитория: в тексте
                                // напоминания подпись формата, а не «ауд. СДО».
                                // «Через N мин» из текста убрано (репорт владельца):
                                // относительное время показывает САМА система в
                                // углу баннера — текст не дублирует. Тип пары
                                // тоже не пишем: он виден цветом карточки.
                                val room = when {
                                    lesson.isRemote -> ", дистанционно (СДО)"
                                    lesson.classrooms.isNotEmpty() ->
                                        ", ауд. ${lesson.classrooms.joinToString(", ")}"
                                    else -> ""
                                }
                                "${lesson.subject} в ${lesson.startTime}$room"
                            }
                            scheduledTargetId = target.id
                        }
                        scheduledTargetId != target.id -> {
                            // Кэш ещё не загружен: не сносить рабочую партию при
                            // временной ошибке. При смене цели после инициализации
                            // убираем старые напоминания, чтобы не оставить чужую цель.
                            if (notificationStateInitialized) {
                                NotificationsManager.reschedule(emptyList(), p.minutes) { "" }
                            }
                            scheduledTargetId = target.id
                        }
                    }
                    notificationStateInitialized = true
                }
            }
        }
    }

    private data class NotificationsPayload(
        val lessons: Map<Int, List<Lesson>>,
        val activeTarget: com.jetbrains.kmpapp.data.model.ScheduleTarget?,
        val enabled: Boolean,
        val minutes: Int,
        val notificationTargetId: Int?,
        val hideAdditionalLessons: Boolean,
        val hiddenLessons: Map<String, Set<String>>
    )

    fun setThemeOverlay(overlay: ThemeOverlay) = storage.setThemeOverlay(overlay)
    fun setMatrixTheme(enabled: Boolean) = storage.setMatrixTheme(enabled)
    fun setHalloweenTheme(enabled: Boolean) = storage.setHalloweenTheme(enabled)
    fun dismissHalloweenSeason() = storage.dismissHalloweenSeason()
    fun clearHalloweenSeasonDismissal() = storage.clearHalloweenSeasonDismissal()
    fun setCheatsAgreed(agreed: Boolean?) = storage.setCheatsAgreed(agreed)
    fun setCheatsBlocked(blocked: Boolean) = storage.setCheatsBlocked(blocked)
    fun setDiagnosticsEnabled(enabled: Boolean) = storage.setDiagnosticsEnabled(enabled)
    fun setAppIcon(name: String) = storage.setAppIcon(name)
    fun setNotificationsEnabled(enabled: Boolean) = storage.setNotificationsEnabled(enabled)
    fun setNotifyMinutesBefore(minutes: Int) = storage.setNotifyMinutesBefore(minutes)
    fun setNotificationsTargetId(targetId: Int?) = storage.setNotificationsTargetId(targetId)
    fun setVpnWarningEnabled(enabled: Boolean) = storage.setVpnWarningEnabled(enabled)
    fun setQrButtonEnabled(enabled: Boolean) = storage.setQrButtonEnabled(enabled)
    fun setAskBeforeNoteDelete(ask: Boolean) = storage.setAskBeforeNoteDelete(ask)
    fun setLessonTypeColor(type: LessonType, color: LessonColor) =
        storage.setLessonTypeColor(type, color)
    fun resetLessonTypeColors() = storage.resetLessonTypeColors()
    fun setCyberpunkVariant(variant: CyberpunkVariant) = storage.setCyberpunkVariant(variant)

    fun setSakuraTheme(enabled: Boolean) {
        storage.setSakuraThemeExclusive(enabled)
    }

    fun setKawaiiTheme(enabled: Boolean) {
        storage.setKawaiiThemeExclusive(enabled)
    }

    fun setCyberpunkTheme(enabled: Boolean) {
        storage.setCyberpunkTheme(enabled)
    }

    /** Матрица потеряна: выключить и убрать из списка найденных тем. */
    fun revokeMatrixUnlock() {
        storage.revokeMatrixUnlock()
    }

    fun hideLesson(lesson: Lesson) {
        val target = storage.selectedTarget.value ?: return
        storage.hideLesson(target, lesson.id)
    }

    fun showLesson(lesson: Lesson) {
        val target = storage.selectedTarget.value ?: return
        storage.showLesson(target, lesson.id)
    }

    init {
        scope.launch {
            storage.selectedTarget.collect { target ->
                if (target != null) {
                    val cached = storage.getLessons(target.id)
                    val hasCached = cached != null && cached.isNotEmpty()
                    val lastSync = storage.getLastSyncTime(target.id)
                    val now = Clock.System.now().toEpochMilliseconds()
                    val isFresh = (now - lastSync) < 30 * 60 * 1000L // 30 minutes TTL

                    if (!hasCached) {
                        // First load for this target: fetch from network with loading indicator
                        refreshSchedule(target, silent = false)
                    } else if (!isFresh) {
                        // Power saving policy:
                        // If system is in Low Power Mode / Battery Saver OR the app is in background,
                        // do not wake up the cellular/Wi-Fi radio for silent background sync! Use cached lessons.
                        val shouldSkipSilentSync = powerManager.isLowPowerMode.value || !powerManager.isAppInForeground.value
                        if (!shouldSkipSilentSync) {
                            refreshSchedule(target, silent = true)
                        }
                    }
                    // Otherwise: cache is fresh (< 30 min), do not make network call!
                }
            }
        }
    }

    suspend fun search(query: String): List<ScheduleTarget> {
        return try {
            api.search(query)
        } catch (e: Exception) {
            println("MireaScheduleApi.search failed: ${e.message}")
            emptyList()
        }
    }

    fun addAndSelectTarget(target: ScheduleTarget) {
        storage.addTarget(target)
        scope.launch {
            refreshSchedule(target, silent = false)
        }
    }

    fun selectTarget(target: ScheduleTarget) {
        storage.selectTarget(target)
    }

    fun removeTarget(targetId: Int) {
        storage.removeTarget(targetId)
        // Каскадное удаление заметок (R2): заметки к парам и предметам
        // этого расписания удаляются вместе с ним.
        lessonNotesStorage.removeNotesForTarget(targetId)
    }

    fun addUserLesson(target: ScheduleTarget, lesson: UserLesson) {
        storage.addUserLesson(target, lesson)
    }

    fun deleteUserLesson(ruleId: String, date: LocalDate, scope: UserLessonDeleteScope) {
        val target = selectedTarget.value ?: return
        storage.deleteUserLesson(target, ruleId, date, scope)
    }

    fun refreshCurrentSchedule() {
        val current = selectedTarget.value ?: return
        scope.launch {
            refreshSchedule(current, silent = false)
        }
    }

    fun refreshTarget(target: ScheduleTarget) {
        scope.launch {
            refreshSchedule(target, silent = false)
        }
    }

    private suspend fun refreshSchedule(target: ScheduleTarget, silent: Boolean = false) {
        if (!silent) {
            _isLoading.value = true
            _errorMessage.value = null
        }
        try {
            val ical = api.getIcal(target.type, target.id)
            val parsedLessons = MireaICalParser.parse(ical)
            // Защита от затирания кэша: пустой результат парсинга
            // (сервер отдал мусор/пустой iCal) не должен стирать
            // сохранённое расписание.
            if (parsedLessons.isEmpty()) {
                throw IOException("Пустой ответ парсера iCal, кэш не затёрт")
            }
            val oldLessons = storage.getLessons(target.id)
            storage.saveLessons(target.id, parsedLessons)
            storage.saveWeekMarkers(target.id, MireaICalParser.parseWeekMarkers(ical))
            // Заметки к парам переживают обновление: при сдвиге пары
            // (дата/звонок изменились) заметка переезжает за ней
            // (remapLessonNotes), а не «отклеивается» навсегда.
            if (oldLessons != null && oldLessons.isNotEmpty()) {
                lessonNotesStorage.remapLessonNotes(target.id, oldLessons, parsedLessons)
            }
            val now = Clock.System.now().toEpochMilliseconds()
            storage.setLastSyncTime(target.id, now)
            _errorMessage.value = null
            if (!silent) {
                _refreshStatus.value = com.jetbrains.kmpapp.data.model.RefreshStatus.Success()
            }

            if (oldLessons != null && oldLessons.isNotEmpty()) {
                val diff = computeDiff(oldLessons, parsedLessons, target)
                if (diff != null) {
                    _activeDiff.value = diff
                }
            }
        } catch (e: Exception) {
            println("refreshSchedule error for ${target.targetTitle}: ${e.message}")
            val code = com.jetbrains.kmpapp.data.model.AppErrorCode.fromException(e)
            _refreshStatus.value = com.jetbrains.kmpapp.data.model.RefreshStatus.Error(code)
            // Only show user-facing full-screen error if there is NO cached data at all
            val cached = storage.getLessons(target.id)
            val hasCached = cached != null && cached.isNotEmpty()
            if (!hasCached) {
                _errorMessage.value = "Ошибка (${code.code}): ${code.shortTitle}"
            }
        } finally {
            if (!silent) {
                _isLoading.value = false
            }
        }
    }

    /**
     * Разворачивает правила повторяющихся пар в карточки на диапазон,
     * покрывающий пейджер дней (±1000 дней от сегодня). Базовая дата
     * добавляется всегда, даже если оказалась за пределами окна.
     */
    private fun expandUserLessons(rules: List<UserLesson>): List<Lesson> {
        if (rules.isEmpty()) return emptyList()
        val today = DateUtils.today()
        val start = today.minus(DatePeriod(days = 1000))
        val end = today.plus(DatePeriod(days = 1000))
        val result = mutableListOf<Lesson>()
        rules.forEach { rule ->
            var day = start
            while (day.dayOfWeek != rule.date.dayOfWeek) {
                day = day.plus(DatePeriod(days = 1))
            }
            while (day <= end) {
                if (rule.occursOn(day)) result += rule.toLesson(day)
                day = day.plus(DatePeriod(days = 7))
            }
            if (rule.date < start || rule.date > end) {
                if (rule.occursOn(rule.date)) result += rule.toLesson(rule.date)
            }
        }
        return result.sortedWith(compareBy({ it.date }, { it.bellNumber }))
    }

    private fun computeDiff(
        oldLessons: List<Lesson>,
        newLessons: List<Lesson>,
        target: ScheduleTarget
    ): ScheduleDiff? {
        val oldMap = oldLessons.groupBy { "${it.date}_${it.bellNumber}_${it.subject.trim().lowercase()}" }
        val newMap = newLessons.groupBy { "${it.date}_${it.bellNumber}_${it.subject.trim().lowercase()}" }

        val items = mutableListOf<LessonDiffItem>()

        for ((key, newGroup) in newMap) {
            val oldGroup = oldMap[key]
            if (oldGroup == null) {
                for (lesson in newGroup) {
                    items.add(
                        LessonDiffItem(
                            type = LessonDiffType.ADDED,
                            date = lesson.date,
                            bellNumber = lesson.bellNumber,
                            subject = lesson.subject,
                            // «СДО» — дистанционный маркер, не аудитория.
                            description = if (lesson.isRemote) {
                                "Новая пара (${lesson.bellNumber} пара, дистанционно · СДО)"
                            } else {
                                "Новая пара (${lesson.bellNumber} пара, ауд. ${lesson.classrooms.joinToString().ifEmpty { "—" }})"
                            }
                        )
                    )
                }
            } else {
                val oldFirst = oldGroup.first()
                val newFirst = newGroup.first()
                val changes = mutableListOf<String>()
                if (oldFirst.classrooms != newFirst.classrooms) {
                    changes.add("ауд: ${oldFirst.classrooms.joinToString().ifEmpty { "—" }} → ${newFirst.classrooms.joinToString().ifEmpty { "—" }}")
                }
                if (oldFirst.teachers != newFirst.teachers) {
                    changes.add("преп: ${oldFirst.teachers.joinToString().ifEmpty { "—" }} → ${newFirst.teachers.joinToString().ifEmpty { "—" }}")
                }
                if (oldFirst.startTime != newFirst.startTime) {
                    changes.add("время: ${oldFirst.startTime} → ${newFirst.startTime}")
                }
                if (changes.isNotEmpty()) {
                    items.add(
                        LessonDiffItem(
                            type = LessonDiffType.MODIFIED,
                            date = newFirst.date,
                            bellNumber = newFirst.bellNumber,
                            subject = newFirst.subject,
                            description = "${newFirst.bellNumber} пара: ${changes.joinToString(", ")}"
                        )
                    )
                }
            }
        }

        for ((key, oldGroup) in oldMap) {
            if (!newMap.containsKey(key)) {
                for (lesson in oldGroup) {
                    items.add(
                        LessonDiffItem(
                            type = LessonDiffType.CANCELLED,
                            date = lesson.date,
                            bellNumber = lesson.bellNumber,
                            subject = lesson.subject,
                            description = "Отменена (${lesson.bellNumber} пара)"
                        )
                    )
                }
            }
        }

        return if (items.isNotEmpty()) {
            ScheduleDiff(
                targetId = target.id,
                targetTitle = target.targetTitle,
                items = items.sortedWith(compareBy({ it.date }, { it.bellNumber }))
            )
        } else null
    }

    fun clearCache() {
        storage.clearCache()
        selectedTarget.value?.let { current ->
            scope.launch {
                refreshSchedule(current, silent = false)
            }
        }
    }

    fun resetAllData() {
        storage.resetAllData()
    }
}
