package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.model.AppVersion
import com.jetbrains.kmpapp.data.review.InstallSource
import com.jetbrains.kmpapp.theme.GlitchText

/** Страница отзывов приложения в RuStore (подтверждена по каталогу). */
private const val RUSTORE_REVIEWS_URL =
    "https://www.rustore.ru/catalog/app/ru.vibemoments.krasava/reviews"

/**
 * Просьба поставить звезду на GitHub и оставить отзыв в магазине.
 *
 * Запрос владельца: показывается на 8-м запуске приложения. Отзыв просим
 * ТОЛЬКО если приложение установлено из магазина, где его можно оставить
 * (RuStore): человеку с APK с GitHub отзыв там оставить негде, и просьба
 * выглядела бы издёвкой. В таком случае остаётся только звезда.
 *
 * Ответ любой кнопкой закрывает просьбу навсегда — навязчивость хуже, чем
 * отсутствие отзыва. «Позже» откладывает на несколько запусков (не больше
 * двух раз, см. ReviewPromptPolicy).
 */
@Composable
fun ReviewPromptDialog(
    installSource: InstallSource,
    onStarClicked: () -> Unit,
    onReviewClicked: () -> Unit,
    onLater: () -> Unit,
    onNeverAsk: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val canReview = installSource.hasStoreListing

    AlertDialog(
        onDismissRequest = onLater,
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            GlitchText(
                text = "Нравится «Красава!»?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                GlitchText(
                    text = if (canReview) {
                        "Приложение делают студенты в свободное время, без рекламы и " +
                            "подписок. Звезда на GitHub и отзыв в RuStore помогают нам " +
                            "видеть, что это нужно, — и находить новых пользователей."
                    } else {
                        "Приложение делают студенты в свободное время, без рекламы и " +
                            "подписок. Звезда на GitHub помогает нам видеть, что это " +
                            "нужно, — и находить новых пользователей."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        runCatching { uriHandler.openUri(AppVersion.GITHUB_REPO_URL) }
                        onStarClicked()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.width(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    GlitchText("Поставить звезду", fontWeight = FontWeight.Bold)
                }
                if (canReview) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching { uriHandler.openUri(RUSTORE_REVIEWS_URL) }
                            onReviewClicked()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlitchText("Оставить отзыв в RuStore", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onLater) {
                GlitchText("Позже")
            }
        },
        dismissButton = {
            TextButton(onClick = onNeverAsk) {
                GlitchText(
                    text = "Не спрашивать",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}