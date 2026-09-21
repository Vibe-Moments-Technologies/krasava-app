package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.jetbrains.kmpapp.data.VkAvatarLoader
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import org.koin.compose.koinInject

data class StudentResource(
    val title: String,
    val description: String,
    val url: String,
    val icon: ImageVector,
    val accentColor: Color
)

val STUDENT_RESOURCES = listOf(
    StudentResource(
        title = "Личный кабинет студента",
        description = "Доступ к оценкам, приказам, договорам и электронным сервисам университета.",
        url = "https://lk.mirea.ru/",
        icon = Icons.Default.AccountCircle,
        accentColor = Color(0xFF007AFF)
    ),
    StudentResource(
        title = "СДО МИРЭА",
        description = "Система дистанционного обучения: курсы, тесты, лекционные материалы и задания.",
        url = "https://online-edu.mirea.ru/",
        icon = Icons.Default.School,
        accentColor = Color(0xFF5856D6)
    ),
    StudentResource(
        title = "Пульс МИРЭА",
        description = "Сервис для отметок посещаемости на парах, контроля успеваемости и баллов БРС.",
        url = "https://pulse.mirea.ru/",
        icon = Icons.Default.MonitorHeart,
        accentColor = Color(0xFFFF2D55)
    ),
    StudentResource(
        title = "Облако студента",
        description = "Корпоративное облачное хранилище Nextcloud для учебных файлов и совместной работы.",
        url = "https://cloud.mirea.ru/",
        icon = Icons.Default.Cloud,
        accentColor = Color(0xFF00C7BE)
    ),
    StudentResource(
        title = "Справочник студента",
        description = "База знаний и инструкций: контакты отделений, регламенты и ответы на вопросы.",
        url = "https://student.mirea.ru/help/ ",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        accentColor = Color(0xFFFF9500)
    ),
    StudentResource(
        title = "Предложение идей университету",
        description = "Платформа студенческих инициатив для предложений по улучшению вуза.",
        url = "https://vote.mirea.ru/",
        icon = Icons.Default.Lightbulb,
        accentColor = Color(0xFFFFCC00)
    )
)

/** Элемент раздела «Другие ресурсы»: папка или ссылка. */
sealed interface ResourceEntry {
    val title: String
}

/** Папка: только название и иконка, без ссылки и описания. */
data class ResourceFolder(
    override val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val children: List<ResourceEntry>
) : ResourceEntry

/**
 * Ссылка внутри папки. [url] == null означает «в разработке»: никуда не ведёт.
 * [fetchAvatarFrom] — страница, с которой нужно подтянуть аватар (для ВК).
 */
data class ResourceLink(
    override val title: String,
    val description: String,
    val symbol: String,
    val url: String?,
    val accentColor: Color,
    val fetchAvatarFrom: String? = null
) : ResourceEntry

private fun vkLink(title: String, description: String, symbol: String, url: String, accent: Color) =
    ResourceLink(
        title = title,
        description = description,
        symbol = symbol,
        url = url,
        accentColor = accent,
        fetchAvatarFrom = url
    )

private fun plannedLink(title: String, description: String, accent: Color) =
    ResourceLink(
        title = title,
        description = description,
        symbol = "загрузка",
        url = null,
        accentColor = accent
    )

private val INSTITUTE_VK_LINKS = listOf(
    vkLink("ИКБ", "Институт кибербезопасности и цифровых технологий", "Слон", "https://vk.ru/ikb_sumirea", Color(0xFF007AFF)),
    vkLink("ИИИ", "Институт искусственного интеллекта", "Робот", "https://vk.ru/iii_sumirea", Color(0xFFAF52DE)),
    vkLink("ИИТ", "Институт информационных технологий", "Панда", "https://vk.ru/it_sumirea", Color(0xFF00C7BE)),
    vkLink("ИТУ", "Институт технологий управления", "Динозавр", "https://vk.ru/itu_sumirea", Color(0xFFFF9500)),
    vkLink("ИПТИП", "Институт перспективных технологий и индустриального программирования", "Лев", "https://vk.ru/iptip_sumirea", Color(0xFF5856D6)),
    vkLink("ИТХТ имени М.В. Ломоносова", "Институт тонких химических технологий имени М.В. Ломоносова", "Феникс", "https://vk.ru/itht_sumirea", Color(0xFFE64A19)),
    vkLink("ИРИ", "Институте радиоэлектроники и информатики", "летучая мышь", "https://vk.ru/iri_sumirea", Color(0xFF34C759)),
    vkLink("КПК", "Колледж программирования и кибербезопасности", "Ворон", "https://vk.ru/college_sumirea", Color(0xFF8E8E93)),
    plannedLink("ПИШ", "Передовые инженерные школы", Color(0xFFFF2D55)),
    plannedLink("Фрязино", "Филиал РТУ МИРЭА в г. Фрязино", Color(0xFF5AC8FA)),
    plannedLink("Ставрополь", "Филиал РТУ МИРЭА в г. Ставрополе", Color(0xFF32ADD6))
)

private val INSTITUTE_TG_LINKS = listOf(
    ResourceLink(
        title = "ИИИ",
        description = "Институт искусственного интеллекта",
        symbol = "Робот",
        url = "https://t.me/iii_sumirea",
        accentColor = Color(0xFFAF52DE)
    ),
    plannedLink("ИКБ", "Институт кибербезопасности и цифровых технологий", Color(0xFF007AFF)),
    plannedLink("ИИТ", "Институт информационных технологий", Color(0xFF00C7BE)),
    plannedLink("ИТУ", "Институт технологий управления", Color(0xFFFF9500)),
    plannedLink("ИПТИП", "Институт перспективных технологий и индустриального программирования", Color(0xFF5856D6)),
    plannedLink("ИТХТ имени М.В. Ломоносова", "Институт тонких химических технологий имени М.В. Ломоносова", Color(0xFFE64A19)),
    plannedLink("ИРИ", "Институте радиоэлектроники и информатики", Color(0xFF34C759)),
    plannedLink("КПК", "Колледж программирования и кибербезопасности", Color(0xFF8E8E93)),
    plannedLink("ПИШ", "Передовые инженерные школы", Color(0xFFFF2D55)),
    plannedLink("Фрязино", "Филиал РТУ МИРЭА в г. Фрязино", Color(0xFF5AC8FA)),
    plannedLink("Ставрополь", "Филиал РТУ МИРЭА в г. Ставрополе", Color(0xFF32ADD6))
)

/** Логотип ВК (Simple Icons, 24×24 — классический знак «VK»). */
internal val VkMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "VkMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color(0xFF2787F5))) {
            moveTo(6.79f, 7.3f)
            horizontalLineTo(4.05f)
            curveTo(0.13f, 6.24f, 3.25f, 9.99f, 8.72f, 9.99f)
            horizontalLineToRelative(0.31f)
            verticalLineToRelative(-3.57f)
            curveTo(2.01f, 0.2f, 3.53f, 1.67f, 4.14f, 3.57f)
            horizontalLineToRelative(2.84f)
            curveTo(-0.78f, -2.84f, -2.83f, -4.41f, -4.11f, -5.01f)
            curveTo(1.28f, -0.74f, 3.08f, -2.54f, 3.51f, -4.98f)
            horizontalLineToRelative(-2.58f)
            curveTo(-0.56f, 1.98f, -2.22f, 3.78f, -3.8f, 3.95f)
            verticalLineTo(7.3f)
            horizontalLineTo(10.5f)
            verticalLineToRelative(6.92f)
            curveTo(-1.6f, -0.4f, -3.62f, -2.34f, -3.71f, -6.92f)
            close()
        }
    }.build()
}

val OTHER_RESOURCES_ROOT = ResourceFolder(
    title = "Другие ресурсы",
    icon = Icons.Default.FolderOpen,
    accentColor = Color(0xFF8E8E93),
    children = listOf(
        ResourceFolder(
            title = "Университет",
            icon = Icons.Default.AccountBalance,
            accentColor = Color(0xFF5856D6),
            children = emptyList()
        ),
        ResourceFolder(
            title = "Институты",
            icon = Icons.Default.Domain,
            accentColor = Color(0xFF007AFF),
            children = listOf(
                ResourceFolder(
                    title = "ВК",
                    icon = VkMark,
                    accentColor = Color(0xFF2787F5),
                    children = INSTITUTE_VK_LINKS
                ),
                ResourceFolder(
                    title = "ТГ",
                    icon = TelegramMark,
                    accentColor = Color(0xFF29A9EB),
                    children = INSTITUTE_TG_LINKS
                )
            )
        ),
        ResourceFolder(
            title = "Студенческие организации",
            icon = Icons.Default.Groups,
            accentColor = Color(0xFFFF2D55),
            children = listOf(
                ResourceFolder(
                    title = "Студенческий союз РТУ МИРЭА",
                    icon = Icons.Default.GroupAdd,
                    accentColor = Color(0xFF00C7BE),
                    children = emptyList()
                )
            )
        )
    )
)

@Composable
fun ResourcesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val avatarLoader: VkAvatarLoader = koinInject()

    // Стек открытых папок раздела «Другие ресурсы»: переход внутрь папки —
    // вперёд, стрелка/системный «назад» — наружу.
    val folderStack = remember { mutableStateListOf<ResourceFolder>() }
    val currentFolder = folderStack.lastOrNull()

    // Внутри папки системный «назад» закрывает папку, на корне — сам экран.
    PlatformBackHandler(enabled = folderStack.isNotEmpty()) {
        if (folderStack.isNotEmpty()) folderStack.removeAt(folderStack.lastIndex)
    }
    PlatformBackHandler(enabled = folderStack.isEmpty(), onBack = onBack)

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
                IconButton(onClick = {
                    if (folderStack.isNotEmpty()) {
                        folderStack.removeAt(folderStack.lastIndex)
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currentFolder?.title ?: "Ресурсы университета",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val folder = currentFolder
            if (folder == null) {
                Text(
                    text = "Официальные цифровые сервисы РТУ МИРЭА, необходимые для учебы и взаимодействия с университетом.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                STUDENT_RESOURCES.forEach { res ->
                    OfficialResourceCard(res = res, uriHandler = uriHandler)
                }

                // «Другие ресурсы» — в самом низу раздела.
                FolderCard(folder = OTHER_RESOURCES_ROOT) {
                    folderStack.add(OTHER_RESOURCES_ROOT)
                }
            } else {
                folder.children.forEach { child ->
                    when (child) {
                        is ResourceFolder -> FolderCard(folder = child) {
                            folderStack.add(child)
                        }
                        is ResourceLink -> ResourceLinkCard(
                            link = child,
                            avatarLoader = avatarLoader,
                            uriHandler = uriHandler
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/** Карточка официального сервиса (верхняя часть экрана ресурсов). */
@Composable
private fun OfficialResourceCard(
    res: StudentResource,
    uriHandler: UriHandler,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(res.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = res.icon,
                        contentDescription = null,
                        tint = res.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = res.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = res.url.removePrefix("https://").removeSuffix("/"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = res.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { uriHandler.openUri(res.url) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Перейти к сервису",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Папка раздела «Другие ресурсы»: название и иконка, без ссылки и описания. */
@Composable
private fun FolderCard(
    folder: ResourceFolder,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(folder.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = folder.icon,
                    contentDescription = null,
                    tint = folder.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = folder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Ссылка внутри папки: оформлена так же, как ресурсы университета. */
@Composable
private fun ResourceLinkCard(
    link: ResourceLink,
    avatarLoader: VkAvatarLoader,
    uriHandler: UriHandler,
    modifier: Modifier = Modifier
) {
    // Аватар с ВК-страницы, если сеть позволила её отдать; иначе — символ.
    var resolvedAvatar by remember(link.fetchAvatarFrom) { mutableStateOf<String?>(null) }
    LaunchedEffect(link.fetchAvatarFrom) {
        val page = link.fetchAvatarFrom
        if (page != null) {
            resolvedAvatar = avatarLoader.resolveAvatar(page)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinkAvatar(link = link, resolvedAvatar = resolvedAvatar)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = link.url?.removePrefix("https://")?.removeSuffix("/") ?: "В разработке",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (link.url != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = link.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (link.url != null) {
                Button(
                    onClick = { uriHandler.openUri(link.url) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = linkButtonLabel(link.url),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // «В разработке»: никуда не ведёт, нажатие в пустоту.
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "В разработке",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkAvatar(link: ResourceLink, resolvedAvatar: String?) {
    if (link.url == null) {
        // Обозначение «загрузка»: аватара ещё нет, ресурс в разработке.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        return
    }

    val remote = resolvedAvatar
    if (remote != null) {
        SubcomposeAsyncImage(
            model = remote,
            contentDescription = "Аватар ${link.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            loading = { SymbolAvatar(symbol = link.symbol, accentColor = link.accentColor) },
            error = { SymbolAvatar(symbol = link.symbol, accentColor = link.accentColor) }
        )
    } else {
        SymbolAvatar(symbol = link.symbol, accentColor = link.accentColor)
    }
}

/** Заглушка аватара: символ из данных (Слон, Робот, Панда и т.п.). */
@Composable
private fun SymbolAvatar(symbol: String, accentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 7.5.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

private fun linkButtonLabel(url: String): String = when {
    url.contains("t.me") -> "Перейти в Telegram"
    url.contains("vk.") || url.contains("vk.ru") -> "Открыть страницу ВК"
    else -> "Перейти"
}