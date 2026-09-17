package com.jetbrains.kmpapp.screens.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.data.analytics.AppAnalytics
import com.jetbrains.kmpapp.data.model.AppVersion
import kotlinx.coroutines.delay

/** Канал проекта в Telegram. */
private const val TELEGRAM_URL = "https://t.me/MIREA_Schedule"

/**
 * Блок-ссылки на соцсети проекта: квадратные кнопки-иконки без подписей.
 * Discord и Boosty — заглушки: показывают тематический тост «скоро».
 */
@Composable
internal fun ProjectSocialLinks() {
    val uriHandler = LocalUriHandler.current
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SocialIcon(
                icon = GitHubMark,
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = "GitHub",
                onClick = {
                    AppAnalytics.logEvent("social_open", mapOf("network" to "github"))
                    uriHandler.openUri(AppVersion.GITHUB_REPO_URL)
                },
                modifier = Modifier.weight(1f)
            )
            SocialIcon(
                icon = TelegramMark,
                tint = Color(0xFF29A9EB),
                contentDescription = "Telegram",
                onClick = {
                    AppAnalytics.logEvent("social_open", mapOf("network" to "telegram"))
                    uriHandler.openUri(TELEGRAM_URL)
                },
                modifier = Modifier.weight(1f)
            )
            SocialIcon(
                icon = DiscordMark,
                tint = Color(0xFF5865F2),
                contentDescription = "Discord",
                onClick = {
                    AppAnalytics.logEvent("social_open", mapOf("network" to "discord"))
                    toastMessage = "Discord-сервер скоро появится"
                },
                modifier = Modifier.weight(1f)
            )
            SocialIcon(
                icon = BoostyMark,
                tint = Color(0xFFF15F2F),
                contentDescription = "Boosty",
                onClick = {
                    AppAnalytics.logEvent("social_open", mapOf("network" to "boosty"))
                    toastMessage = "Поддержка разработчиков скоро появится"
                },
                modifier = Modifier.weight(1f)
            )
        }
        ComingSoonToast(
            message = toastMessage,
            onDismiss = { toastMessage = null },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Лёгкий тост в теме приложения: fade-анимация, автоскрытие ~2.5с, тап
 * закрывает. Проще и тише системного снекбара, который рвёт тему.
 */
@Composable
private fun ComingSoonToast(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2500)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = message ?: "",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SocialIcon(
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
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

/** Логотип Discord (Simple Icons, 24×24 — точный путь). */
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
            curveTo(19.481f, 3.9763f, 18.6063f, 3.6952f, 17.699f, 3.5093f)
            curveTo(17.6129f, 3.4946f, 17.5404f, 3.5405f, 17.5066f, 3.6193f)
            curveTo(17.2956f, 3.9946f, 17.0619f, 4.4841f, 16.8983f, 4.8688f)
            curveTo(15.0536f, 4.5926f, 13.2183f, 4.5926f, 11.4115f, 4.8688f)
            curveTo(11.2479f, 4.4755f, 11.0057f, 3.9946f, 10.7938f, 3.6193f)
            curveTo(10.7679f, 3.5523f, 10.6953f, 3.5093f, 10.6093f, 3.5093f)
            curveTo(9.7019f, 3.6942f, 8.8282f, 3.9753f, 7.9914f, 4.3698f)
            curveTo(7.972f, 4.3778f, 7.9554f, 4.3914f, 7.9473f, 4.4097f)
            curveTo(5.7359f, 7.71f, 5.1299f, 10.9219f, 5.4234f, 14.0955f)
            curveTo(5.4273f, 14.1149f, 5.4371f, 14.1341f, 5.4525f, 14.1462f)
            curveTo(6.5434f, 14.9467f, 7.6006f, 15.4294f, 8.6415f, 15.7493f)
            curveTo(8.7276f, 15.7749f, 8.8165f, 15.7424f, 8.8637f, 15.6671f)
            curveTo(9.239f, 15.0673f, 9.5717f, 14.4324f, 9.8541f, 13.7661f)
            curveTo(9.8961f, 13.682f, 9.8541f, 13.5838f, 9.768f, 13.5504f)
            curveTo(9.1496f, 13.3157f, 8.5609f, 13.0294f, 7.9931f, 12.7042f)
            curveTo(7.8978f, 12.6482f, 7.8902f, 12.513f, 7.9772f, 12.448f)
            curveTo(8.0862f, 12.3669f, 8.196f, 12.2824f, 8.3004f, 12.1987f)
            curveTo(8.3496f, 12.1588f, 8.4181f, 12.1506f, 8.4752f, 12.1767f)
            curveTo(10.7527f, 13.2164f, 13.2208f, 13.2164f, 15.4625f, 12.1767f)
            curveTo(15.5196f, 12.1497f, 15.5881f, 12.1579f, 15.6383f, 12.1971f)
            curveTo(15.7427f, 12.2808f, 15.8525f, 12.3669f, 15.9624f, 12.4471f)
            curveTo(16.0494f, 12.5121f, 16.0428f, 12.6473f, 15.9465f, 12.7033f)
            curveTo(15.3787f, 13.0343f, 14.7891f, 13.3196f, 14.1716f, 13.5534f)
            curveTo(14.0855f, 13.5868f, 14.0435f, 13.686f, 14.0865f, 13.7691f)
            curveTo(14.3779f, 14.4344f, 14.7106f, 15.0693f, 15.0769f, 15.6682f)
            curveTo(15.1241f, 15.7435f, 15.214f, 15.7759f, 15.3001f, 15.7493f)
            curveTo(16.348f, 15.4294f, 17.4062f, 14.9467f, 18.4961f, 14.1462f)
            curveTo(18.5137f, 14.1332f, 18.5247f, 14.1158f, 18.5287f, 14.0955f)
            curveTo(18.8212f, 10.9229f, 18.2152f, 7.711f, 16.0037f, 4.4107f)
            curveTo(16.0037f, 4.4107f, 15.9961f, 4.3975f, 15.9811f, 4.3917f)
            close()
            moveTo(8.02f, 15.3312f)
            curveTo(7.2611f, 15.3312f, 6.642f, 14.6419f, 6.642f, 13.7906f)
            curveTo(6.642f, 12.9393f, 7.248f, 12.25f, 8.02f, 12.25f)
            curveTo(8.8001f, 12.25f, 9.407f, 12.9484f, 9.398f, 13.7906f)
            curveTo(9.398f, 14.6419f, 8.792f, 15.3312f, 8.02f, 15.3312f)
            close()
            moveTo(15.9918f, 15.3312f)
            curveTo(15.233f, 15.3312f, 14.6139f, 14.6419f, 14.6139f, 13.7906f)
            curveTo(14.6139f, 12.9393f, 15.2199f, 12.25f, 15.9918f, 12.25f)
            curveTo(16.772f, 12.25f, 17.3789f, 12.9484f, 17.3699f, 13.7906f)
            curveTo(17.3699f, 14.6419f, 16.7639f, 15.3312f, 15.9918f, 15.3312f)
            close()
        }
    }.build()
}

/** Логотип Boosty (Simple Icons, 24×24 — точный путь). */
private val BoostyMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "BoostyMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFFF15F2F))) {
            moveTo(2.661f, 14.337f)
            lineTo(6.801f, 0f)
            lineTo(13.163f, 0f)
            lineTo(11.88f, 4.444f)
            lineTo(11.842f, 4.521f)
            lineTo(8.464f, 16.254f)
            lineTo(11.614f, 16.254f)
            curveTo(10.293f, 19.543f, 9.264f, 22.121f, 8.528f, 23.987f)
            curveTo(2.712f, 23.924f, 1.086f, 19.759f, 2.508f, 14.832f)
            close()
            moveTo(8.554f, 24f)
            lineTo(16.224f, 12.965f)
            lineTo(12.974f, 12.965f)
            lineTo(15.804f, 5.892f)
            curveTo(20.656f, 6.4f, 22.941f, 10.222f, 21.595f, 14.844f)
            curveTo(20.16f, 19.81f, 14.344f, 24f, 8.68f, 24f)
            lineTo(8.553f, 24f)
            close()
        }
    }.build()
}
