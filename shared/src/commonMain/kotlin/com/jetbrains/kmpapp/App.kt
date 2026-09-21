package com.jetbrains.kmpapp

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.analytics.AnalyticsEvents
import com.jetbrains.kmpapp.data.analytics.AppAnalytics
import com.jetbrains.kmpapp.data.analytics.platformName
import com.jetbrains.kmpapp.data.model.AppVersion
import com.jetbrains.kmpapp.data.model.ThemeMode
import com.jetbrains.kmpapp.screens.components.AppTab
import com.jetbrains.kmpapp.screens.components.FloatingDock
import com.jetbrains.kmpapp.screens.compare.CompareScheduleScreen
import com.jetbrains.kmpapp.screens.compare.CompareScheduleViewModel
import com.jetbrains.kmpapp.screens.map.MapScreen
import com.jetbrains.kmpapp.screens.notes.NotesScreen
import com.jetbrains.kmpapp.screens.notes.NotesViewModel
import com.jetbrains.kmpapp.screens.other.OtherScreen
import com.jetbrains.kmpapp.screens.other.OtherViewModel
import com.jetbrains.kmpapp.screens.rooms.FreeRoomsScreen
import com.jetbrains.kmpapp.screens.rooms.FreeRoomsViewModel
import com.jetbrains.kmpapp.screens.schedule.ScheduleScreen
import com.jetbrains.kmpapp.screens.schedule.ScheduleViewModel
import com.jetbrains.kmpapp.screens.services.ServicesScreen
import com.jetbrains.kmpapp.screens.services.ServicesViewModel
import com.jetbrains.kmpapp.screens.other.isServiceScreen
import com.jetbrains.kmpapp.screens.tasks.TasksScreen
import com.jetbrains.kmpapp.screens.tasks.TasksViewModel
import com.jetbrains.kmpapp.theme.CyberpunkDarkColors
import com.jetbrains.kmpapp.theme.CyberpunkLightColors
import com.jetbrains.kmpapp.theme.MatrixDarkColors
import com.jetbrains.kmpapp.theme.MatrixLightColors
import com.jetbrains.kmpapp.theme.SakuraDarkColors
import com.jetbrains.kmpapp.theme.SakuraLightColors
import com.jetbrains.kmpapp.theme.ThemeOverlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E5BB0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF555F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E3F8),
    onSecondaryContainer = Color(0xFF121C2B),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFFF0F3F9),
    surfaceContainerHigh = Color(0xFFE8EDF5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468A),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD9E3F8),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceContainer = Color(0xFF1D2026),
    surfaceContainerHigh = Color(0xFF282A30)
)

@Composable
fun App() {
    val repository: ScheduleRepository = koinInject()
    val themeMode by repository.themeMode.collectAsState()
    val themeOverlay by repository.themeOverlay.collectAsState()
    val dockTabs by repository.dockTabs.collectAsState()
    val selectedTarget by repository.selectedTarget.collectAsState()
    val analyticsConsent by repository.analyticsConsent.collectAsState()

    val scheduleViewModel: ScheduleViewModel = koinViewModel()
    val otherViewModel: OtherViewModel = koinViewModel()
    val freeRoomsViewModel: FreeRoomsViewModel = koinViewModel()
    val tasksViewModel: TasksViewModel = koinViewModel()
    val compareViewModel: CompareScheduleViewModel = koinViewModel()
    val notesViewModel: NotesViewModel = koinViewModel()
    val servicesViewModel: ServicesViewModel = koinViewModel()

    // Аналитика: одна стартовая метрика среза аудитории + трекеры изменений.
    // dock_config — каждый слот отдельным параметром: в панели Metrica
    // такое строится в графики, в отличие от строки через запятую.
    LaunchedEffect(Unit) {
        val params = mutableMapOf(
            "target_type" to (selectedTarget?.type?.name ?: "none")
        )
        // Срез по версиям: видно, на чём сидит аудитория. dev/contrib не
        // шлём — статистику иначе забивают наши же тестовые сборки;
        // stable/beta/rc различимы суффиксом версии.
        val channel = AppVersion.BUILD_CHANNEL
        if (channel == "stable" || channel == "beta" || channel == "rc") {
            params["version"] = AppVersion.VERSION_NAME
            params["platform"] = platformName()
        }
        AppAnalytics.logEvent(AnalyticsEvents.SESSION_OPEN, params)
    }
    LaunchedEffect(dockTabs) {
        val params = mutableMapOf("count" to dockTabs.size.toString())
        dockTabs.forEachIndexed { index, tab -> params["slot_${index + 1}"] = tab.name }
        AppAnalytics.logEvent(AnalyticsEvents.SESSION_DOCK_CONFIG, params)
    }
    LaunchedEffect(selectedTarget) {
        selectedTarget?.let { AppAnalytics.logEvent(AnalyticsEvents.SCHEDULE_TARGET_TYPE, mapOf("type" to it.type.name)) }
    }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colors = when (themeOverlay) {
        ThemeOverlay.SAKURA -> if (isDark) SakuraDarkColors else SakuraLightColors
        ThemeOverlay.CYBERPUNK -> if (isDark) CyberpunkDarkColors else CyberpunkLightColors
        ThemeOverlay.MATRIX -> if (isDark) MatrixDarkColors else MatrixLightColors
        ThemeOverlay.NONE -> if (isDark) DarkColors else LightColors
    }

    MaterialTheme(colorScheme = colors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var currentTab by remember { mutableStateOf(AppTab.SCHEDULE) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                try {
                    if (!repository.isLowPowerMode.value) {
                        otherViewModel.checkForUpdates()
                    }
                } catch (_: Throwable) {}
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Crossfade(targetState = currentTab) { tab ->
                    when (tab) {
                        AppTab.SCHEDULE -> {
                            ScheduleScreen(viewModel = scheduleViewModel)
                        }
                        AppTab.FREE_ROOMS -> {
                            FreeRoomsScreen(viewModel = freeRoomsViewModel)
                        }
                        AppTab.TASKS -> {
                            TasksScreen(viewModel = tasksViewModel)
                        }
                        AppTab.MAP -> {
                            MapScreen()
                        }
                        AppTab.NOTES -> {
                            NotesScreen(viewModel = notesViewModel)
                        }
                        AppTab.COMPARE -> {
                            CompareScheduleScreen(viewModel = compareViewModel)
                        }
                        AppTab.SERVICES -> {
                            ServicesScreen(
                                viewModel = servicesViewModel,
                                dockTabs = dockTabs
                            )
                        }
                        AppTab.OTHER -> {
                            OtherScreen(
                                viewModel = otherViewModel,
                                onNavigateToTab = { currentTab = it }
                            )
                        }
                    }
                }

                val density = LocalDensity.current
                val isImeVisible = WindowInsets.ime.getBottom(density) > 0

                if (!isImeVisible) {
                    // Стрелка «назад» в доке: у вкладки с открытым сервисом
                    // иконка раздела меняется на «назад» (тап = возврат).
                    val servicesActiveService by servicesViewModel.activeService.collectAsState()
                    val otherSubScreen by otherViewModel.activeSubScreen.collectAsState()
                    val backModeTab = when {
                        currentTab == AppTab.SERVICES && servicesActiveService != null -> AppTab.SERVICES
                        currentTab == AppTab.OTHER && otherSubScreen.isServiceScreen -> AppTab.OTHER
                        else -> null
                    }
                    FloatingDock(
                        currentTab = currentTab,
                        onTabSelected = {
                            currentTab = it
                            AppAnalytics.logEvent(AnalyticsEvents.NAV_TAB_OPEN, mapOf("tab" to it.name))
                        },
                        onTabReselected = { tab ->
                            when (tab) {
                                AppTab.SCHEDULE -> {
                                    scheduleViewModel.selectLessonForDetail(null)
                                }
                                AppTab.FREE_ROOMS -> {
                                    freeRoomsViewModel.selectRoomForDetail(null)
                                }
                                AppTab.TASKS -> {}
                                AppTab.MAP -> {}
                                AppTab.NOTES -> {}
                                AppTab.COMPARE -> {}
                                AppTab.SERVICES -> {
                                    servicesViewModel.closeService()
                                }
                                AppTab.OTHER -> {
                                    otherViewModel.resetToRoot()
                                }
                            }
                        },
                        tabs = dockTabs,
                        backModeTab = backModeTab,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            // Единый гейт при первом запуске. Без подтверждения приложением
            // пользоваться нельзя — поэтому у диалога нет кнопки отказа.
            if (analyticsConsent == null) {
                ConsentDialog(onAccept = { repository.setAnalyticsConsent(true) })
            }
        }
    }
}

/** Гейт согласия при первом запуске: без принятия приложение не открывается. */
@Composable
private fun ConsentDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                "Привет! 👋",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Для улучшения приложения мы собираем некоторые " +
                        "анонимизированные диагностические и аналитические данные.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                // Обязательный пункт
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        "Диагностика сбоев и ошибок — обязательна, " +
                            "помогает находить и исправлять проблемы.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Опциональный пункт
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 8.dp))
                    Column {
                        Text(
                            "Аналитика использования — необязательна, " +
                                "помогает понимать, какие функции важны.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Можно отключить в любой момент в настройках.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Продолжить")
            }
        }
    )
}
