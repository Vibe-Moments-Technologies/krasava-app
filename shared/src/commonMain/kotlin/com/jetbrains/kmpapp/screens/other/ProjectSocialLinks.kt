package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.jetbrains.kmpapp.data.analytics.AppAnalytics
import com.jetbrains.kmpapp.data.model.AppVersion

/** Канал проекта в Telegram. */
private const val TELEGRAM_URL = "https://t.me/MIREA_Schedule"

/**
 * Блок-ссылки на соцсети проекта. Сам блок намеренно без границ и фона —
 * визуально это просто ряд квадратных кнопок внизу страницы.
 */
@Composable
internal fun ProjectSocialLinks() {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SocialButton(
            icon = GitHubMark,
            label = "GitHub",
            tint = MaterialTheme.colorScheme.onSurface,
            onClick = {
                AppAnalytics.logEvent("social_open", mapOf("network" to "github"))
                uriHandler.openUri(AppVersion.GITHUB_REPO_URL)
            },
            modifier = Modifier.weight(1f)
        )
        SocialButton(
            icon = TelegramMark,
            label = "Telegram",
            tint = Color(0xFF29A9EB),
            onClick = {
                AppAnalytics.logEvent("social_open", mapOf("network" to "telegram"))
                uriHandler.openUri(TELEGRAM_URL)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SocialButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
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
