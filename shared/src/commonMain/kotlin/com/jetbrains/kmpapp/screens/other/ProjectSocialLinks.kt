package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.data.analytics.AppAnalytics
import com.jetbrains.kmpapp.data.model.AppVersion
import kotlinx.coroutines.launch

/** Канал проекта в Telegram. */
private const val TELEGRAM_URL = "https://t.me/MIREA_Schedule"

/**
 * Блок-ссылки на соцсети проекта: квадратные кнопки-иконки без подписей
 * (по иконкам и так понятно). Discord и Boosty — заглушки «скоро».
 */
@Composable
internal fun ProjectSocialLinks() {
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                icon = GitHubMark,
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = "GitHub",
                onClick = {
                    AppAnalytics.logEvent("social_open", mapOf("network" to "github"))
                    uriHandler.openUri(AppVersion.GITHUB_REPO_URL)
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                icon = TelegramMark,
                tint = Color(0xFF29A9EB),
                contentDescription = "Telegram",
                onClick = {
                    AppAnalytics.logEvent("social_open", mapOf("network" to "telegram"))
                    uriHandler.openUri(TELEGRAM_URL)
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                icon = DiscordMark,
                tint = Color(0xFF5865F2),
                contentDescription = "Discord",
                onClick = {
                    AppAnalytics.logEvent("social_open", mapOf("network" to "discord"))
                    scope.launch { snackbar.showSnackbar("Discord-сервер скоро появится") }
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                icon = BoostyMark,
                tint = Color(0xFFF15F2F),
                contentDescription = "Boosty",
                onClick = {
                    AppAnalytics.logEvent("social_open", mapOf("network" to "boosty"))
                    scope.launch { snackbar.showSnackbar("Поддержка разработчиков скоро появится") }
                },
                modifier = Modifier.weight(1f)
            )
        }
        // снекбар поверх кнопок: экран не скроллится, обычный Scaffold-хост
        // снизу перекрыт доком
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun IconButton(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Логотип GitHub (Simple Icons, 24×24). */
private val GitHubMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "GitHubMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF181717))) {
            moveTo(12f, 0.297f)
            curveTo(5.37f, 0.297f, 0f, 5.667f, 0f, 12.297f)
            curveTo(0f, 17.6f, 3.438f, 22.097f, 8.205f, 23.682f)
            curveTo(8.805f, 23.795f, 9.025f, 23.422f, 9.025f, 23.1f)
            curveTo(9.025f, 22.812f, 9.015f, 22.05f, 9.015f, 21.15f)
            curveTo(5.67f, 21.88f, 4.965f, 19.535f, 4.965f, 19.535f)
            curveTo(4.41f, 18.13f, 3.63f, 17.76f, 3.63f, 17.76f)
            curveTo(2.55f, 17.025f, 3.705f, 17.04f, 3.705f, 17.04f)
            curveTo(4.89f, 17.115f, 5.515f, 18.27f, 5.515f, 18.27f)
            curveTo(6.585f, 20.1f, 8.295f, 19.575f, 9.05f, 19.26f)
            curveTo(9.15f, 18.48f, 9.48f, 17.955f, 9.84f, 17.655f)
            curveTo(7.185f, 17.355f, 4.395f, 16.35f, 4.395f, 11.745f)
            curveTo(4.395f, 10.425f, 4.845f, 9.345f, 5.595f, 8.505f)
            curveTo(5.475f, 8.19f, 5.085f, 6.975f, 5.685f, 5.31f)
            curveTo(5.685f, 5.31f, 6.705f, 4.98f, 8.97f, 6.525f)
            curveTo(9.945f, 6.255f, 10.95f, 6.12f, 12f, 6.12f)
            curveTo(13.05f, 6.12f, 14.1f, 6.27f, 15.03f, 6.525f)
            curveTo(17.295f, 4.98f, 18.315f, 5.31f, 18.315f, 5.31f)
            curveTo(18.915f, 6.975f, 18.51f, 8.19f, 18.39f, 8.505f)
            curveTo(19.155f, 9.345f, 19.605f, 10.425f, 19.605f, 11.745f)
            curveTo(19.605f, 16.365f, 16.8f, 17.355f, 14.13f, 17.655f)
            curveTo(14.58f, 18.015f, 14.985f, 18.735f, 14.985f, 19.83f)
            curveTo(14.985f, 21.39f, 14.97f, 22.65f, 14.97f, 23.1f)
            curveTo(14.97f, 23.43f, 15.18f, 23.79f, 15.795f, 23.685f)
            curveTo(20.565f, 22.11f, 24f, 17.61f, 24f, 12.3f)
            curveTo(24f, 5.667f, 18.63f, 0.297f, 12f, 0.297f)
            close()
        }
    }.build()
}

/** Логотип Telegram (Simple Icons, 24×24). */
private val TelegramMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "TelegramMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF29A9EB))) {
            moveTo(11.944f, 0f)
            curveTo(5.351f, 0f, 0f, 5.351f, 0f, 11.944f)
            curveTo(0f, 18.538f, 5.351f, 23.889f, 11.944f, 23.889f)
            curveTo(18.538f, 23.889f, 23.889f, 18.538f, 23.889f, 11.944f)
            curveTo(23.889f, 5.351f, 18.538f, 0f, 11.944f, 0f)
            close()
            moveTo(17.488f, 8.161f)
            curveTo(17.31f, 10.037f, 16.539f, 14.590f, 16.146f, 16.692f)
            curveTo(15.981f, 17.579f, 15.652f, 17.877f, 15.337f, 17.905f)
            curveTo(14.650f, 17.968f, 14.129f, 17.452f, 13.464f, 17.016f)
            curveTo(12.421f, 16.333f, 11.834f, 15.908f, 10.822f, 15.241f)
            curveTo(9.650f, 14.468f, 10.404f, 14.045f, 11.068f, 13.354f)
            curveTo(11.241f, 13.173f, 14.275f, 10.411f, 14.334f, 10.163f)
            curveTo(14.341f, 10.131f, 14.349f, 10.012f, 14.275f, 9.960f)
            curveTo(14.202f, 9.907f, 14.098f, 9.926f, 14.02f, 9.941f)
            curveTo(13.913f, 9.963f, 12.256f, 11.057f, 9.045f, 13.224f)
            curveTo(8.572f, 13.549f, 8.143f, 13.707f, 7.757f, 13.698f)
            curveTo(7.331f, 13.688f, 6.514f, 13.457f, 5.905f, 13.259f)
            curveTo(5.157f, 13.017f, 4.564f, 12.889f, 4.616f, 12.478f)
            curveTo(4.643f, 12.264f, 4.936f, 12.045f, 5.493f, 11.822f)
            curveTo(8.936f, 10.322f, 11.231f, 9.333f, 12.379f, 8.855f)
            curveTo(15.658f, 7.491f, 16.34f, 7.253f, 16.784f, 7.245f)
            curveTo(16.882f, 7.243f, 17.102f, 7.268f, 17.245f, 7.383f)
            curveTo(17.365f, 7.480f, 17.398f, 7.611f, 17.414f, 7.703f)
            curveTo(17.434f, 7.816f, 17.447f, 8.007f, 17.437f, 8.161f)
            close()
        }
    }.build()
}

/** Логотип Discord (Simple Icons, 24×24). */
private val DiscordMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "DiscordMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF5865F2))) {
            moveTo(20.317f, 4.3698f)
            curveTo(19.788f, 4.1283f, 19.231f, 3.9273f, 18.654f, 3.7723f)
            curveTo(18.596f, 3.7608f, 18.538f, 3.7878f, 18.508f, 3.8408f)
            curveTo(18.443f, 3.9568f, 18.371f, 4.1038f, 18.321f, 4.2223f)
            curveTo(17.7f, 4.1293f, 17.054f, 4.0758f, 16.4f, 4.0758f)
            curveTo(15.746f, 4.0758f, 15.1f, 4.1293f, 14.479f, 4.2223f)
            curveTo(14.429f, 4.1013f, 14.355f, 3.9568f, 14.29f, 3.8408f)
            curveTo(14.26f, 3.7883f, 14.202f, 3.7613f, 14.144f, 3.7723f)
            curveTo(13.568f, 3.9268f, 13.011f, 4.1278f, 12.481f, 4.3698f)
            curveTo(12.457f, 4.3798f, 12.436f, 4.3983f, 12.423f, 4.4223f)
            curveTo(11.398f, 5.9558f, 10.895f, 7.4898f, 10.821f, 9.0088f)
            curveTo(10.819f, 9.0363f, 10.832f, 9.0628f, 10.853f, 9.0793f)
            curveTo(11.377f, 9.4648f, 11.885f, 9.7053f, 12.383f, 9.8648f)
            curveTo(12.428f, 9.8783f, 12.476f, 9.8623f, 12.505f, 9.8233f)
            curveTo(12.645f, 9.6323f, 12.770f, 9.4313f, 12.877f, 9.2203f)
            curveTo(12.908f, 9.1588f, 12.877f, 9.0868f, 12.812f, 9.0623f)
            curveTo(12.619f, 8.9893f, 12.435f, 8.9013f, 12.258f, 8.8033f)
            curveTo(12.186f, 8.7613f, 12.181f, 8.6583f, 12.248f, 8.6103f)
            curveTo(12.284f, 8.5838f, 12.32f, 8.5563f, 12.355f, 8.5288f)
            curveTo(13.635f, 9.9383f, 15.014f, 10.6358f, 16.4f, 10.6358f)
            curveTo(17.786f, 10.6358f, 19.165f, 9.9383f, 20.445f, 8.5288f)
            curveTo(20.481f, 8.5563f, 20.516f, 8.5838f, 20.552f, 8.6103f)
            curveTo(20.619f, 8.6583f, 20.615f, 8.7613f, 20.542f, 8.8033f)
            curveTo(20.365f, 8.9013f, 20.181f, 8.9893f, 19.988f, 9.0623f)
            curveTo(19.923f, 9.0868f, 19.892f, 9.1588f, 19.923f, 9.2203f)
            curveTo(20.031f, 9.4313f, 20.155f, 9.6323f, 20.295f, 9.8233f)
            curveTo(20.324f, 9.8623f, 20.373f, 9.8783f, 20.418f, 9.8648f)
            curveTo(20.918f, 9.7053f, 21.426f, 9.4648f, 21.949f, 9.0793f)
            curveTo(21.97f, 9.0628f, 21.983f, 9.0358f, 21.981f, 9.0083f)
            curveTo(21.911f, 7.4898f, 21.408f, 5.9558f, 20.382f, 4.4223f)
            curveTo(20.37f, 4.3983f, 20.349f, 4.3798f, 20.325f, 4.3698f)
            close()
            moveTo(14.521f, 8.0588f)
            curveTo(14.521f, 8.4783f, 14.215f, 8.8188f, 13.835f, 8.8188f)
            curveTo(13.455f, 8.8188f, 13.149f, 8.4783f, 13.149f, 8.0588f)
            curveTo(13.149f, 7.6393f, 13.455f, 7.2988f, 13.835f, 7.2988f)
            curveTo(14.215f, 7.2988f, 14.521f, 7.6393f, 14.521f, 8.0588f)
            close()
            moveTo(19.492f, 8.0588f)
            curveTo(19.492f, 8.4783f, 19.186f, 8.8188f, 18.806f, 8.8188f)
            curveTo(18.426f, 8.8188f, 18.12f, 8.4783f, 18.12f, 8.0588f)
            curveTo(18.12f, 7.6393f, 18.426f, 7.2988f, 18.806f, 7.2988f)
            curveTo(19.186f, 7.2988f, 19.492f, 7.6393f, 19.492f, 8.0588f)
            close()
        }
    }.build()
}

/** Логотип Boosty (Simple Icons, 24×24). */
private val BoostyMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "BoostyMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFFF15F2F))) {
            moveTo(11.944f, 0f)
            curveTo(5.799f, 0f, 1.276f, 2.442f, 1.276f, 2.442f)
            curveTo(1.276f, 2.442f, 0.733f, 3.66f, 0.733f, 5.13f)
            curveTo(0.733f, 6.6f, 1.247f, 7.73f, 2.229f, 8.657f)
            curveTo(3.79f, 10.14f, 6.66f, 10.774f, 8.892f, 10.774f)
            lineTo(6.646f, 12.636f)
            curveTo(6.279f, 12.94f, 6.279f, 13.62f, 6.646f, 13.924f)
            lineTo(7.926f, 14.992f)
            curveTo(8.293f, 15.296f, 8.293f, 15.977f, 7.926f, 16.281f)
            lineTo(6.646f, 17.349f)
            curveTo(6.279f, 17.653f, 6.279f, 18.333f, 6.646f, 18.637f)
            lineTo(7.926f, 19.705f)
            curveTo(8.293f, 20.009f, 8.293f, 20.69f, 7.926f, 20.994f)
            lineTo(6.468f, 22.211f)
            curveTo(6.248f, 22.394f, 6.139f, 22.754f, 6.139f, 23.036f)
            curveTo(6.139f, 23.59f, 6.59f, 24f, 7.16f, 24f)
            lineTo(11.944f, 24f)
            curveTo(18.46f, 24f, 23.0f, 18.75f, 23.0f, 12.25f)
            curveTo(23.0f, 12.25f, 23.267f, 0f, 11.944f, 0f)
            close()
            moveTo(11.944f, 20.78f)
            curveTo(8.2f, 20.78f, 5.75f, 17.4f, 5.75f, 12.25f)
            curveTo(5.75f, 7.1f, 8.2f, 3.72f, 11.944f, 3.72f)
            curveTo(15.688f, 3.72f, 18.138f, 7.1f, 18.138f, 12.25f)
            curveTo(18.138f, 17.4f, 15.688f, 20.78f, 11.944f, 20.78f)
            close()
        }
    }.build()
}
