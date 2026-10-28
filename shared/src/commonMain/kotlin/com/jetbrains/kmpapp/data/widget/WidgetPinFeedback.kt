package com.jetbrains.kmpapp.data.widget

import kotlin.time.Clock

/**
 * Обратная связь закрепления виджета.
 *
 * Конфигуратор виджета открывается success-callback'ом запроса
 * закрепления (см. WidgetPinning): если пользователь подтвердил
 * добавление — конфигуратор стартует и отмечает здесь момент открытия
 * (androidApp пишет метку, shared читает). Экран настроек после
 * запроса ждёт ограниченное время: метка не обновилась — системный
 * диалог был подавлен (OEM-запрет, «нет прав»), и вместо молчания
 * показывается предупреждение-диалог поверх экрана.
 */
object WidgetPinFeedback {
    /** Момент (epoch-мс) последнего запроса закрепления виджета. */
    var lastPinRequestedAtMillis: Long = 0L
    /** Момент (epoch-мс) последнего открытия конфигуратора виджета. */
    var lastConfigureOpenedAtMillis: Long = 0L

    /** Отметить запрос закрепления (звонится экраном настроек). */
    fun markPinRequested() {
        lastPinRequestedAtMillis = Clock.System.now().toEpochMilliseconds()
    }
}
