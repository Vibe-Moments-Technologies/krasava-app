package com.jetbrains.kmpapp.screens.other

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * Экспериментальные параметры.
 *
 * Адаптировано под общий стиль страниц настроек: [SettingsPageScaffold]
 * и [SettingsGroup]. Полный сброс сюда НЕ выносим — он остаётся в отладке.
 */
@Composable
fun ExperimentalSettingsScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullResourcesEnabled by viewModel.fullResourcesEnabled.collectAsState()

    SettingsPageScaffold(
        title = "Экспериментальные параметры",
        onBack = onBack,
        modifier = modifier,
        scrollKey = "experimental"
    ) {
        SettingsGroup(title = "Ресурсы") {
            SettingsSwitchRow(
                title = "Полные ресурсы",
                subtitle = "Показывать содержимое студенческих организаций",
                checked = fullResourcesEnabled,
                onCheckedChange = viewModel::setFullResourcesEnabled
            )
        }
    }
}
