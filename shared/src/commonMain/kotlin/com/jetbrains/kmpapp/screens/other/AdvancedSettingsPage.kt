package com.jetbrains.kmpapp.screens.other

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * Страница «Дополнительно».
 *
 * Виджет перенесён в «Уведомления и виджеты»: его цель по умолчанию совпадает
 * с целью уведомлений, поэтому настройки логично держать рядом (Android).
 * Эксперименты и полный сброс сюда НЕ выносим — они остаются в скрытом
 * меню отладки (решение владельца).
 */
@Composable
fun AdvancedSettingsPage(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vpnWarningEnabled by viewModel.vpnWarningEnabled.collectAsState()

    SettingsPageScaffold(title = "Дополнительно", onBack = onBack, modifier = modifier) {
        SettingsGroup(title = "Сеть") {
            SettingsSwitchRow(
                title = "Предупреждения о VPN",
                subtitle = "Показывать предупреждение, если VPN может помешать обновлению расписания",
                checked = vpnWarningEnabled,
                onCheckedChange = viewModel::setVpnWarningEnabled
            )
        }
    }
}
