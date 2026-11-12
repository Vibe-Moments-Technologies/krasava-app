package com.jetbrains.kmpapp.screens.other
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.theme.LocalNewYearEnabled
import org.jetbrains.compose.resources.painterResource

@Composable
fun TeamScreen(
    onBack: () -> Unit,
    onGitHubClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current
    val newYearActive = LocalNewYearEnabled.current

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                GlitchText(
                    text = "Команда проекта",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // Группировка по отделам: карточки идут в порядке [teamDepartments];
        // новый отдел = строка в списке + члены с этим department.
        val byDepartment = projectTeam.groupBy { it.department }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp, vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (department in teamDepartments) {
                val members = byDepartment[department] ?: continue
                item(key = "dept_$department") {
                    GlitchText(
                        text = department,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(members, key = { it.name }) { member ->
                    TeamMemberCard(
                        member = member,
                        newYearActive = newYearActive,
                        onGitHubClick = onGitHubClick
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TeamAvatar(member: TeamMember, newYearActive: Boolean) {
    val hat = member.newYearHat.takeIf { newYearActive }
    Box(
        modifier = Modifier.size(
            width = 52.dp,
            height = if (hat == null) 52.dp else 64.dp
        ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .then(
                    if (member.avatar == null) {
                        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (member.avatar != null) {
                Image(
                    painter = painterResource(member.avatar),
                    contentDescription = "Аватар ${member.name}",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Заглушка-инициал: для участников без фото (например, отдел «Карты»).
                GlitchText(
                    text = member.name.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (hat != null) {
            NewYearHat(
                hat = hat,
                modifier = Modifier
                    .size(width = 52.dp, height = 40.dp)
                    .align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun NewYearHat(hat: TeamNewYearHat, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (hat) {
            TeamNewYearHat.SANTA -> drawSantaHat()
            TeamNewYearHat.SNEGUROCHKA -> drawSnegurochkaHat()
        }
    }
}

private fun DrawScope.drawSantaHat() {
    val w = size.width
    val h = size.height
    val fur = Color(0xFFFFF8EC)
    val furShadow = Color(0xFF9EC4DE).copy(alpha = 0.5f)

    val cone = Path().apply {
        moveTo(w * 0.10f, h * 0.55f)
        cubicTo(w * 0.12f, h * 0.30f, w * 0.36f, h * 0.02f, w * 0.58f, h * 0.06f)
        cubicTo(w * 0.76f, h * 0.09f, w * 0.86f, h * 0.17f, w * 0.89f, h * 0.25f)
        cubicTo(w * 0.82f, h * 0.32f, w * 0.79f, h * 0.42f, w * 0.77f, h * 0.55f)
        close()
    }
    drawPath(cone, Color(0xFFD83B37))
    drawLine(
        color = Color.White.copy(alpha = 0.24f),
        start = Offset(w * 0.20f, h * 0.50f),
        end = Offset(w * 0.48f, h * 0.08f),
        strokeWidth = 1.3.dp.toPx(),
        cap = StrokeCap.Round
    )

    drawRoundRect(
        color = furShadow,
        topLeft = Offset(w * 0.06f, h * 0.445f),
        size = Size(w * 0.86f, h * 0.23f),
        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
    )
    drawRoundRect(
        color = fur,
        topLeft = Offset(w * 0.06f, h * 0.43f),
        size = Size(w * 0.86f, h * 0.23f),
        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
    )
    drawCircle(
        color = furShadow,
        radius = w * 0.095f,
        center = Offset(w * 0.89f, h * 0.26f)
    )
    drawCircle(
        color = fur,
        radius = w * 0.09f,
        center = Offset(w * 0.89f, h * 0.24f)
    )
}

private fun DrawScope.drawSnegurochkaHat() {
    val w = size.width
    val h = size.height
    val fur = Color(0xFFFFF8EC)
    val furShadow = Color(0xFF9EC4DE).copy(alpha = 0.5f)

    val crown = Path().apply {
        moveTo(w * 0.10f, h * 0.55f)
        cubicTo(w * 0.12f, h * 0.32f, w * 0.30f, h * 0.03f, w * 0.50f, h * 0.05f)
        cubicTo(w * 0.70f, h * 0.03f, w * 0.88f, h * 0.32f, w * 0.90f, h * 0.55f)
        close()
    }
    drawPath(crown, Color(0xFF54B8E8))

    val inner = Path().apply {
        moveTo(w * 0.21f, h * 0.53f)
        cubicTo(w * 0.23f, h * 0.34f, w * 0.36f, h * 0.15f, w * 0.50f, h * 0.15f)
        cubicTo(w * 0.64f, h * 0.15f, w * 0.77f, h * 0.34f, w * 0.79f, h * 0.53f)
        close()
    }
    drawPath(inner, Color(0xFFC9F1FF).copy(alpha = 0.85f))
    drawPath(
        path = crown,
        color = Color.White.copy(alpha = 0.4f),
        style = Stroke(width = 0.9.dp.toPx())
    )

    val center = Offset(w * 0.50f, h * 0.29f)
    val arm = w * 0.095f
    repeat(3) { index ->
        rotate(degrees = 60f * index, pivot = center) {
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(center.x - arm, center.y),
                end = Offset(center.x + arm, center.y),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }

    drawRoundRect(
        color = furShadow,
        topLeft = Offset(w * 0.06f, h * 0.445f),
        size = Size(w * 0.86f, h * 0.23f),
        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
    )
    drawRoundRect(
        color = fur,
        topLeft = Offset(w * 0.06f, h * 0.43f),
        size = Size(w * 0.86f, h * 0.23f),
        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
    )
}

@Composable
private fun TeamMemberCard(
    member: TeamMember,
    newYearActive: Boolean,
    onGitHubClick: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamAvatar(member = member, newYearActive = newYearActive)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    GlitchText(
                        text = member.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    GlitchText(
                        text = member.role,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    member.email?.let { email ->
                        Spacer(modifier = Modifier.height(3.dp))
                        // Контактный e-mail: тап открывает почтовый клиент (mailto).
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { uriHandler.openUri("mailto:$email") }
                                .padding(vertical = 1.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            GlitchText(
                                text = email,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Личные ссылки — компактные иконки-чипы; вся карточка больше не кликабельна.
            if (member.links.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    member.links.forEach { link ->
                        val (icon, tint, label) = when (link.type) {
                            TeamLinkType.GITHUB -> Triple(
                                GitHubMark,
                                MaterialTheme.colorScheme.onSurface,
                                "GitHub"
                            )
                            TeamLinkType.TELEGRAM -> Triple(
                                TelegramMark,
                                Color(0xFF29A9EB),
                                "Telegram"
                            )
                            TeamLinkType.OTHER -> Triple(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                MaterialTheme.colorScheme.primary,
                                "Ссылка"
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable {
                                    if (link.type == TeamLinkType.GITHUB) {
                                        onGitHubClick(link.url)
                                    }
                                    uriHandler.openUri(link.url)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = tint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            GlitchText(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
