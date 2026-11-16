package com.jetbrains.kmpapp.screens.other
import com.jetbrains.kmpapp.theme.GlitchText

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.jetbrains.kmpapp.data.VkAvatarLoader
import com.jetbrains.kmpapp.screens.components.LayeredNavHost
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import kmp_app_template.shared.generated.resources.Res
import kmp_app_template.shared.generated.resources.res_logo_cloud
import kmp_app_template.shared.generated.resources.res_logo_fund
import kmp_app_template.shared.generated.resources.res_logo_ibc
import kmp_app_template.shared.generated.resources.res_logo_lk
import kmp_app_template.shared.generated.resources.res_logo_mirea
import kmp_app_template.shared.generated.resources.res_logo_pulse
import kmp_app_template.shared.generated.resources.res_logo_sdo
import kmp_app_template.shared.generated.resources.res_logo_student
import kmp_app_template.shared.generated.resources.res_logo_vote
import kmp_app_template.shared.generated.resources.res_project_bal
import kmp_app_template.shared.generated.resources.res_project_education
import kmp_app_template.shared.generated.resources.res_project_miti
import kmp_app_template.shared.generated.resources.res_project_starteen
import kmp_app_template.shared.generated.resources.res_project_student_god
import kmp_app_template.shared.generated.resources.res_project_team
import kmp_app_template.shared.generated.resources.res_project_vri_les
import kmp_app_template.shared.generated.resources.res_social_apriori
import kmp_app_template.shared.generated.resources.res_social_arctic
import kmp_app_template.shared.generated.resources.res_social_esports
import kmp_app_template.shared.generated.resources.res_social_ovk
import kmp_app_template.shared.generated.resources.res_social_profkom
import kmp_app_template.shared.generated.resources.res_social_startup
import kmp_app_template.shared.generated.resources.res_social_sumirea
import kmp_app_template.shared.generated.resources.res_social_sumirea_site
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

data class StudentResource(
    val title: String,
    val description: String,
    val url: String,
    val icon: ImageVector,
    val accentColor: Color,
    /** Фон плитки иконки; null — светлый оттенок [accentColor]. */
    val iconBackground: Color? = null,
    /** Логотип сервиса; если задан — показывается вместо [icon]. */
    val logo: DrawableResource? = null
)

val STUDENT_RESOURCES = listOf(
    StudentResource(
        title = "Сайт РТУ МИРЭА",
        description = "Официальный сайт университета: новости, структура, документы и контакты.",
        url = "https://mirea.ru/",
        icon = Icons.Default.Language,
        accentColor = Color(0xFF007AFF),
        iconBackground = Color.White,
        logo = Res.drawable.res_logo_mirea
    ),
    StudentResource(
        title = "Личный кабинет студента",
        description = "Доступ к оценкам, приказам, договорам и электронным сервисам университета.",
        url = "https://lk.mirea.ru/",
        icon = Icons.Default.AccountCircle,
        accentColor = Color(0xFF007AFF),
        iconBackground = Color.White,
        logo = Res.drawable.res_logo_lk
    ),
    StudentResource(
        title = "СДО МИРЭА",
        description = "Система дистанционного обучения: курсы, тесты, лекционные материалы и задания.",
        url = "https://online-edu.mirea.ru/",
        icon = Icons.Default.School,
        accentColor = Color(0xFF5856D6),
        iconBackground = Color.White,
        logo = Res.drawable.res_logo_sdo
    ),
    StudentResource(
        title = "Пульс МИРЭА",
        description = "Сервис для отметок посещаемости на парах, контроля успеваемости и баллов БРС.",
        url = "https://pulse.mirea.ru/",
        icon = Icons.Default.MonitorHeart,
        accentColor = Color(0xFFFF2D55),
        iconBackground = Color.White,
        logo = Res.drawable.res_logo_pulse
    ),
    StudentResource(
        title = "Справочник студента",
        description = "База знаний и инструкций: контакты отделений, регламенты и ответы на вопросы.",
        url = "https://student.mirea.ru/help/",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        accentColor = Color(0xFFFF9500),
        iconBackground = Color.White,
        logo = Res.drawable.res_logo_student
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
    /** Вторая ссылка в карточке: для сообществ — Telegram рядом с ВК. */
    val secondaryUrl: String? = null,
    val fetchAvatarFrom: String? = null,
    /** Иконка вместо эмодзи-символа; null — рисуем [symbol]. */
    val icon: ImageVector? = null,
    /** Логотип ресурса; показывается вместо [icon]/[symbol]. */
    val logo: DrawableResource? = null,
    /** Логотип — квадратная аватарка: заполняет круг, а не вписывается в него. */
    val logoIsAvatar: Boolean = false,
    /** Фон аватара-иконки; null — светлый оттенок [accentColor]. */
    val iconBackground: Color? = null
) : ResourceEntry

private fun vkLink(
    title: String,
    description: String,
    symbol: String,
    url: String,
    accent: Color,
    logo: DrawableResource? = null,
    icon: ImageVector? = null,
    secondaryUrl: String? = null
) = ResourceLink(
    title = title,
    description = description,
    symbol = symbol,
    url = url,
    accentColor = accent,
    secondaryUrl = secondaryUrl,
    fetchAvatarFrom = url,
    icon = icon,
    logo = logo,
    logoIsAvatar = logo != null
)

private fun tgLink(
    title: String,
    description: String,
    symbol: String,
    url: String,
    accent: Color,
    logo: DrawableResource? = null,
    icon: ImageVector? = null
) = ResourceLink(
    title = title,
    description = description,
    symbol = symbol,
    url = url,
    accentColor = accent,
    fetchAvatarFrom = url,
    icon = icon,
    logo = logo,
    logoIsAvatar = logo != null
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
    vkLink("ИКБ", "Институт кибербезопасности и цифровых технологий", "🐘", "https://vk.ru/ikb_sumirea", Color(0xFF007AFF)),
    vkLink("ИИИ", "Институт искусственного интеллекта", "🤖", "https://vk.ru/iii_sumirea", Color(0xFF34C759)),
    vkLink("ИИТ", "Институт информационных технологий", "🐼", "https://vk.ru/it_sumirea", Color(0xFF1C1C1E)),
    vkLink("ИТУ", "Институт технологий управления", "🦕", "https://vk.ru/itu_sumirea", Color(0xFFFF3B30)),
    vkLink("ИПТИП", "Институт перспективных технологий и индустриального программирования", "🦁", "https://vk.ru/iptip_sumirea", Color(0xFFFFCC00)),
    vkLink("ИТХТ имени М.В. Ломоносова", "Институт тонких химических технологий имени М.В. Ломоносова", "🐦‍🔥", "https://vk.ru/itht_sumirea", Color(0xFFFF2D55)),
    vkLink("ИРИ", "Институте радиоэлектроники и информатики", "🦇", "https://vk.ru/iri_sumirea", Color(0xFFAF52DE)),
    plannedLink("ИРИП", "Институт радиоэлектроники и индустриального программирования", Color(0xFF5856D6)),
    vkLink("КПК", "Колледж программирования и кибербезопасности", "🐦‍⬛", "https://vk.ru/college_sumirea", Color(0xFFFF9500)),
    plannedLink("ПИШ", "Передовые инженерные школы", Color(0xFFFF2D55)),
    plannedLink("Фрязино", "Филиал РТУ МИРЭА в г. Фрязино", Color(0xFF5AC8FA)),
    plannedLink("Ставрополь", "Филиал РТУ МИРЭА в г. Ставрополе", Color(0xFF32ADD6))
)

private val INSTITUTE_TG_LINKS = listOf(
    ResourceLink(
        title = "ИИИ",
        description = "Институт искусственного интеллекта",
        symbol = "🤖",
        url = "https://t.me/iii_sumirea",
        accentColor = Color(0xFF34C759)
    ),
    plannedLink("ИКБ", "Институт кибербезопасности и цифровых технологий", Color(0xFF007AFF)),
    plannedLink("ИИТ", "Институт информационных технологий", Color(0xFF1C1C1E)),
    plannedLink("ИТУ", "Институт технологий управления", Color(0xFFFF3B30)),
    plannedLink("ИПТИП", "Институт перспективных технологий и индустриального программирования", Color(0xFFFFCC00)),
    plannedLink("ИТХТ имени М.В. Ломоносова", "Институт тонких химических технологий имени М.В. Ломоносова", Color(0xFFFF2D55)),
    plannedLink("ИРИ", "Институте радиоэлектроники и информатики", Color(0xFFAF52DE)),
    plannedLink("ИРИП", "Институт радиоэлектроники и индустриального программирования", Color(0xFF5856D6)),
    plannedLink("КПК", "Колледж программирования и кибербезопасности", Color(0xFFFF9500)),
    plannedLink("ПИШ", "Передовые инженерные школы", Color(0xFFFF2D55)),
    plannedLink("Фрязино", "Филиал РТУ МИРЭА в г. Фрязино", Color(0xFF5AC8FA)),
    plannedLink("Ставрополь", "Филиал РТУ МИРЭА в г. Ставрополе", Color(0xFF32ADD6))
)

/** Логотип ВК (Font Awesome 6 brands «vk», 448×512 — классический знак «VK»). */
internal val VkMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "VkMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 448f,
        viewportHeight = 512f
    ).apply {
        path(fill = SolidColor(Color(0xFF0077FF))) {
            moveTo(31.4907f, 63.4907f)
            curveTo(0f, 94.9813f, 0f, 145.671f, 0f, 247.04f)
            verticalLineTo(264.96f)
            curveTo(0f, 366.329f, 0f, 417.019f, 31.4907f, 448.509f)
            curveTo(62.9813f, 480f, 113.671f, 480f, 215.04f, 480f)
            horizontalLineTo(232.96f)
            curveTo(334.329f, 480f, 385.019f, 480f, 416.509f, 448.509f)
            curveTo(448f, 417.019f, 448f, 366.329f, 448f, 264.96f)
            verticalLineTo(247.04f)
            curveTo(448f, 145.671f, 448f, 94.9813f, 416.509f, 63.4907f)
            curveTo(385.019f, 32f, 334.329f, 32f, 232.96f, 32f)
            horizontalLineTo(215.04f)
            curveTo(113.671f, 32f, 62.9813f, 32f, 31.4907f, 63.4907f)
            close()
            moveTo(75.6f, 168.267f)
            horizontalLineTo(126.747f)
            curveTo(128.427f, 253.76f, 166.133f, 289.973f, 196f, 297.44f)
            verticalLineTo(168.267f)
            horizontalLineTo(244.16f)
            verticalLineTo(242f)
            curveTo(273.653f, 238.827f, 304.64f, 205.227f, 315.093f, 168.267f)
            horizontalLineTo(363.253f)
            curveTo(359.313f, 187.435f, 351.46f, 205.583f, 340.186f, 221.579f)
            curveTo(328.913f, 237.574f, 314.461f, 251.071f, 297.733f, 261.227f)
            curveTo(316.41f, 270.499f, 332.907f, 283.63f, 346.132f, 299.751f)
            curveTo(359.357f, 315.873f, 369.01f, 334.618f, 374.453f, 354.747f)
            horizontalLineTo(321.44f)
            curveTo(316.555f, 337.262f, 306.614f, 321.61f, 292.865f, 309.754f)
            curveTo(279.117f, 297.899f, 262.173f, 290.368f, 244.16f, 288.107f)
            verticalLineTo(354.747f)
            horizontalLineTo(238.373f)
            curveTo(136.267f, 354.747f, 78.0267f, 284.747f, 75.6f, 168.267f)
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
            children = listOf(
                ResourceLink(
                    title = "Ректорат",
                    description = "Руководство университета, состав ректората, контакты и приёмные часы.",
                    symbol = "🏛️",
                    url = "https://www.mirea.ru/about/administration/rektorat/",
                    accentColor = Color(0xFF5856D6),
                    icon = Icons.Default.AccountBalance,
                    logo = Res.drawable.res_logo_mirea,
                    iconBackground = Color.White
                ),
                ResourceLink(
                    title = "Облако студента",
                    description = "Корпоративное облачное хранилище Nextcloud для учебных файлов и совместной работы.",
                    symbol = "☁️",
                    url = "https://cloud.mirea.ru/",
                    accentColor = Color(0xFF00C7BE),
                    icon = Icons.Default.Cloud,
                    logo = Res.drawable.res_logo_cloud,
                    iconBackground = Color.White
                ),
                ResourceLink(
                    title = "Предложение идей университету",
                    description = "Платформа студенческих инициатив для предложений по улучшению вуза.",
                    symbol = "💡",
                    url = "https://vote.mirea.ru/",
                    accentColor = Color(0xFFFFCC00),
                    icon = Icons.Default.Lightbulb,
                    logo = Res.drawable.res_logo_vote,
                    iconBackground = Color.White
                ),
                ResourceLink(
                    title = "Информационный библиотечный центр",
                    description = "Электронные каталоги, учебники, научные базы и сервисы библиотеки РТУ МИРЭА.",
                    symbol = "📚",
                    url = "https://ibc.mirea.ru/",
                    accentColor = Color(0xFFFF9500),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    logo = Res.drawable.res_logo_ibc,
                    iconBackground = Color.White
                ),
                ResourceLink(
                    title = "Фонд целевого капитала",
                    description = "Поддержка университета, стипендий и образовательных проектов через фонд целевого капитала.",
                    symbol = "🌳",
                    url = "https://fund.mirea.ru/",
                    accentColor = Color(0xFF007AFF),
                    icon = Icons.Default.Park,
                    logo = Res.drawable.res_logo_fund,
                    iconBackground = Color.White
                )
            )
        ),
        ResourceFolder(
            title = "Институты",
            icon = Icons.Default.Domain,
            accentColor = Color(0xFF007AFF),
            children = listOf(
                ResourceFolder(
                    title = "ВК",
                    icon = VkMark,
                    accentColor = Color(0xFF0077FF),
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
                    accentColor = Color(0xFF007AFF),
                    children = listOf(
                        vkLink(
                            title = "Сообщество",
                            description = "Главные новости Студенческого союза, анонсы событий и возможности для студентов РТУ МИРЭА.",
                            symbol = "🤝",
                            url = "https://vk.ru/sumirea",
                            accent = Color(0xFF007AFF),
                            logo = Res.drawable.res_social_sumirea,
                            secondaryUrl = "https://t.me/sumirea"
                        ),
                        ResourceLink(
                            title = "Сайт",
                            description = "Официальный сайт Студенческого союза РТУ МИРЭА",
                            symbol = "🌐",
                            url = "https://sumirea.ru/",
                            accentColor = Color(0xFF007AFF),
                            logo = Res.drawable.res_social_sumirea_site,
                            iconBackground = Color.White
                        ),
                        vkLink(
                            title = "ОВК",
                            description = "Отчётно-выборные Конференции Студенческого союза МИРЭА",
                            symbol = "🗳️",
                            url = "https://vk.ru/ovksumirea",
                            accent = Color(0xFF007AFF),
                            logo = Res.drawable.res_social_ovk
                        ),
                        ResourceFolder(
                            title = "Структуры",
                            icon = Icons.Default.Groups,
                            accentColor = Color(0xFF34C759),
                            children = listOf(
                                ResourceFolder(
                                    title = "Подразделения",
                                    icon = Icons.Default.AccountTree,
                                    accentColor = Color(0xFF007AFF),
                                    children = listOf(
                                        ResourceFolder(
                                            title = "Волонтёрский центр РТУ МИРЭА",
                                            icon = Icons.Default.VolunteerActivism,
                                            accentColor = Color(0xFF34C759),
                                            children = listOf(
                                                vkLink(
                                                    title = "Волонтёрский центр во ВКонтакте",
                                                    description = "Новости центра, наборы на события и полезные материалы для добровольцев.",
                                                    symbol = "💚",
                                                    url = "https://vk.com/vcrtumirea",
                                                    accent = Color(0xFF0077FF),
                                                    icon = Icons.Default.VolunteerActivism
                                                ),
                                                tgLink(
                                                    title = "Волонтёрский центр в Telegram",
                                                    description = "Анонсы акций, наборы волонтёров и новости добровольческого движения РТУ МИРЭА.",
                                                    symbol = "📣",
                                                    url = "https://t.me/vc_rtumirea",
                                                    accent = Color(0xFF29A9EB),
                                                    icon = Icons.Default.Campaign
                                                ),
                                                tgLink(
                                                    title = "Волонтёрский штаб в Telegram",
                                                    description = "Оперативные объявления, дежурства и координация волонтёров на мероприятиях.",
                                                    symbol = "🗣️",
                                                    url = "https://t.me/volunteer_sch",
                                                    accent = Color(0xFF34C759),
                                                    icon = Icons.Default.Groups
                                                ),
                                                ResourceLink(
                                                    title = "Волонтёрская беседа во ВКонтакте",
                                                    description = "Открытая беседа для общения волонтёров, обмена опытом и поиска единомышленников.",
                                                    symbol = "🫶",
                                                    url = "https://vk.me/join/ATwVdJzu5TEfATv8MQF0mgD2Fmaa71bZkbM=",
                                                    accentColor = Color(0xFF0077FF),
                                                    icon = Icons.Default.Forum
                                                )
                                            )
                                        )
                                    )
                                ),
                                ResourceFolder(
                                    title = "Клубы по интересам",
                                    icon = Icons.Default.Interests,
                                    accentColor = Color(0xFFFF2D55),
                                    children = listOf(
                                        tgLink(
                                            title = "Клуб Спортивной Мафии",
                                            description = "Вечера спортивной мафии: игры для новичков и опытных, разборы партий и дружеская атмосфера.",
                                            symbol = "🕵️",
                                            url = "https://t.me/mirea_mafia",
                                            accent = Color(0xFF8E24AA),
                                            icon = Icons.Default.Casino
                                        )
                                    )
                                ),
                                ResourceFolder(
                                    title = "Отряды",
                                    icon = Icons.Default.MilitaryTech,
                                    accentColor = Color(0xFF34C759),
                                    children = emptyList()
                                )
                            )
                        ),
                        ResourceFolder(
                            title = "Проекты",
                            icon = Icons.Default.RocketLaunch,
                            accentColor = Color(0xFFFF9500),
                            children = listOf(
                                vkLink(
                                    title = "Лагерь-семинар «Мы – команда»",
                                    description = "Выездной лагерь-семинар студенческого актива: командообразование, сцена, мастер-классы и встреча с администрацией университета.",
                                    symbol = "🏕️",
                                    url = "https://vk.ru/wearetheteam",
                                    accent = Color(0xFF34C759),
                                    logo = Res.drawable.res_project_team
                                ),
                                ResourceFolder(
                                    title = "ВРИ «Лес»",
                                    icon = Icons.Default.Forest,
                                    accentColor = Color(0xFF2E8B57),
                                    children = listOf(
                                        vkLink(
                                            title = "ВКонтакте",
                                            description = "Новости и сообщество военно-ролевой игры «Лес»: сражения по правилам «Зарницы», магия и квесты.",
                                            symbol = "🌲",
                                            url = "https://vk.ru/vri_les",
                                            accent = Color(0xFF2E8B57),
                                            logo = Res.drawable.res_project_vri_les
                                        ),
                                        tgLink(
                                            title = "Telegram",
                                            description = "Telegram-канал проекта: анонсы, новости и организация выездов.",
                                            symbol = "🌲",
                                            url = "https://t.me/vri_les",
                                            accent = Color(0xFF2E8B57),
                                            logo = Res.drawable.res_project_vri_les
                                        )
                                    )
                                ),
                                ResourceFolder(
                                    title = "МИТИ «Наследие Леса»",
                                    icon = Icons.Default.Nature,
                                    accentColor = Color(0xFFA2845E),
                                    children = listOf(
                                        vkLink(
                                            title = "ВКонтакте",
                                            description = "Молодёжная историко-туристическая игра по мотивам древнерусских сказаний и славянской мифологии.",
                                            symbol = "🌳",
                                            url = "https://vk.ru/miti_nasledielesa",
                                            accent = Color(0xFFA2845E),
                                            logo = Res.drawable.res_project_miti
                                        ),
                                        tgLink(
                                            title = "Telegram",
                                            description = "Telegram-канал проекта: анонсы, новости и подготовка к выезду.",
                                            symbol = "🌳",
                                            url = "https://t.me/miti_nasledielesa",
                                            accent = Color(0xFFA2845E),
                                            logo = Res.drawable.res_project_miti
                                        )
                                    )
                                ),
                                vkLink(
                                    title = "Традиционный бал РТУ МИРЭА",
                                    description = "Ежегодный студенческий бал университета: вальс, исторические танцы, этикет и тематическая программа.",
                                    symbol = "💃",
                                    url = "https://vk.ru/bal_rtumirea",
                                    accent = Color(0xFFAF52DE),
                                    logo = Res.drawable.res_project_bal
                                ),
                                vkLink(
                                    title = "Студент и преподаватель года",
                                    description = "Конкурс университета, который отмечает лучших студентов, преподавателей и наставников по итогам года.",
                                    symbol = "🏆",
                                    url = "https://vk.ru/studentgodartumirea",
                                    accent = Color(0xFFFF9500),
                                    logo = Res.drawable.res_project_student_god
                                ),
                                vkLink(
                                    title = "Учебные субботы",
                                    description = "Проект Студенческого союза: занятия по сложным дисциплинам от старших студентов и материалы для подготовки к сессии.",
                                    symbol = "📚",
                                    url = "https://vk.ru/education_sumirea",
                                    accent = Color(0xFF007AFF),
                                    logo = Res.drawable.res_project_education
                                ),
                                vkLink(
                                    title = "Стартин",
                                    description = "Традиционный танцевальный конкурс между институтами и колледжем РТУ МИРЭА.",
                                    symbol = "🕺",
                                    url = "https://vk.ru/starteen_sumirea",
                                    accent = Color(0xFFFF2D55),
                                    logo = Res.drawable.res_project_starteen
                                ),
                                tgLink(
                                    title = "Что? Где? Когда?",
                                    description = "Интеллектуальный клуб университета: тренировки, турниры и игры в формате «Что? Где? Когда?».",
                                    symbol = "🦉",
                                    url = "https://t.me/chogdekogda_rtumirea",
                                    accent = Color(0xFF5856D6)
                                )
                            )
                        )
                    )
                ),
                ResourceFolder(
                    title = "Профсоюзная организация РТУ МИРЭА",
                    icon = Icons.Default.Handshake,
                    accentColor = Color(0xFF5856D6),
                    children = listOf(
                        vkLink(
                            title = "Сообщество",
                            description = "Новости профсоюзной организации, защита прав студентов и анонсы мероприятий.",
                            symbol = "🤝",
                            url = "https://vk.ru/rtuprofkom",
                            accent = Color(0xFF5856D6),
                            logo = Res.drawable.res_social_profkom,
                            secondaryUrl = "https://t.me/mireaprofkom"
                        ),
                        vkLink(
                            title = "Априори",
                            description = "Педагогический отряд",
                            symbol = "🎓",
                            url = "https://vk.ru/apriori.moscow",
                            accent = Color(0xFF34C759),
                            logo = Res.drawable.res_social_apriori
                        )
                    )
                ),
                ResourceFolder(
                    title = "Студенческое научное общество РТУ МИРЭА",
                    icon = Icons.Default.Science,
                    accentColor = Color(0xFF34C759),
                    children = emptyList()
                ),
                ResourceFolder(
                    title = "Киберзона",
                    icon = Icons.Default.SportsEsports,
                    accentColor = Color(0xFF0B0B0F),
                    children = listOf(
                        ResourceLink(
                            title = "Бронирование зон",
                            description = "Бронирование VR-зон, компьютерных классов и настольных игр в киберпространстве РТУ МИРЭА.",
                            symbol = "🐉",
                            url = "https://esports.mirea.app/",
                            accentColor = Color.White,
                            icon = CyberzoneDragon,
                            iconBackground = Color(0xFF0B0B0F)
                        ),
                        vkLink(
                            title = "Сообщество",
                            description = "Новости киберспорта, турниры, анонсы мероприятий и жизнь киберзоны РТУ МИРЭА.",
                            symbol = "🎮",
                            url = "https://vk.ru/esports.mirea",
                            accent = Color(0xFF0077FF),
                            logo = Res.drawable.res_social_esports,
                            secondaryUrl = "https://t.me/mirea_esports"
                        ),
                        tgLink(
                            title = "Настольные игры",
                            description = "Telegram-канал клуба настольных игр киберзоны РТУ МИРЭА.",
                            symbol = "🎲",
                            url = "https://t.me/mirea_tablegames",
                            accent = Color(0xFFFF9500),
                            icon = Icons.Default.Casino
                        ),
                        ResourceLink(
                            title = "Вакансии",
                            description = "Вакансии и работа в киберзоне РТУ МИРЭА.",
                            symbol = "💼",
                            url = "https://job.cyberzone.dev",
                            accentColor = Color(0xFF34C759),
                            icon = Icons.Default.Work
                        )
                    )
                ),
                ResourceFolder(
                    title = "Стартап-клуб РТУ МИРЭА",
                    icon = Icons.Default.RocketLaunch,
                    accentColor = Color(0xFFFF9500),
                    children = listOf(
                        ResourceLink(
                            title = "Информация",
                            description = "Информация с официального сайта о Стартап-клубе РТУ МИРЭА",
                            symbol = "🔗",
                            url = "https://www.mirea.ru/news/v-rtu-mirea-sostoyalos-otkrytie-startap-kluba/?ysclid=mucklirr211037720",
                            accentColor = Color(0xFFFF9500),
                            icon = Icons.Default.Newspaper,
                            logo = Res.drawable.res_social_startup,
                            logoIsAvatar = true
                        ),
                        tgLink(
                            title = "Сообщество",
                            description = "Тг сообщество Стартап-клуба РТУ МИРЭА",
                            symbol = "💬",
                            url = "https://t.me/StartupClubRTUMIREA",
                            accent = Color(0xFF29A9EB),
                            logo = Res.drawable.res_social_startup
                        ),
                        tgLink(
                            title = "Беседа",
                            description = "Тг беседа Стартап-клуба РТУ МИРЭА",
                            symbol = "🗨️",
                            url = "https://t.me/StartupClub_RTUMIREA",
                            accent = Color(0xFF29A9EB),
                            logo = Res.drawable.res_social_startup
                        )
                    )
                ),
                ResourceFolder(
                    title = "Центр культуры и творчества",
                    icon = Icons.Default.Palette,
                    accentColor = Color(0xFFAF52DE),
                    children = emptyList()
                ),
                vkLink(
                    title = "Команда Арктики",
                    description = "Студенческое объединение «Команда Арктики» РТУ МИРЭА",
                    symbol = "🐻‍❄️",
                    url = "https://vk.ru/arcticcenter",
                    accent = Color(0xFF5AC8FA),
                    logo = Res.drawable.res_social_arctic
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

    // Путь открытых папок — штатная схема «родитель под дочерним», как в
    // TasksScreen/ServicesScreen: каждая папка — слой вложенного
    // LayeredNavHost; назад (жест, стрелка, системная кнопка) снимает
    // последний слой с общим выездом анимации.
    val folderStack = remember { mutableStateListOf<ResourceFolder>() }
    val stackSnapshot = folderStack.toList().takeIf { it.isNotEmpty() }
    // Папка, которая закроется по текущему «назад». Снимаем именно её, даже
    // если во время анимации успели открыть следующую: иначе поздний pop
    // удалил бы только что открытую папку и вернул предыдущую.
    var pendingPopFolder by remember { mutableStateOf<ResourceFolder?>(null) }
    // Скролл корня переживает переходы по папкам: хост перерисовывает корень
    // в другом слое (под дочерней папкой).
    val rootScrollState = rememberScrollState()
    // У каждой папки — собственный скролл. Слот экрана переиспользуется
    // между слоями, и общий rememberScrollState переносил прокрутку
    // предыдущей папки на новую.
    val folderScrollStates = remember { mutableMapOf<ResourceFolder, ScrollState>() }

    // Системный «назад» на корне — выход с экрана. Когда папка открыта,
    // поверх встаёт её собственный PlatformBackHandler (Android: LIFO).
    PlatformBackHandler(onBack = onBack)

    LayeredNavHost(
        screen = stackSnapshot,
        parentScreen = stackSnapshot?.dropLast(1)?.takeIf { it.isNotEmpty() },
        onBackStarted = { path ->
            @Suppress("UNCHECKED_CAST")
            pendingPopFolder = (path as? List<ResourceFolder>)?.lastOrNull()
        },
        onBackToParent = {
            val target = pendingPopFolder
            pendingPopFolder = null
            if (target != null) {
                val index = folderStack.indexOfLast { it === target }
                if (index >= 0) folderStack.removeAt(index)
            } else if (folderStack.isNotEmpty()) {
                folderStack.removeAt(folderStack.lastIndex)
            }
        },
        rootContent = {
            ResourcesRootContent(
                scrollState = rootScrollState,
                uriHandler = uriHandler,
                onBack = onBack,
                onOpenFolder = { folderStack.add(it) }
            )
        },
        screenContent = { path, back ->
            @Suppress("UNCHECKED_CAST")
            val folder = (path as List<ResourceFolder>).last()
            PlatformBackHandler(onBack = back)
            FolderContentScreen(
                folder = folder,
                scrollState = folderScrollStates.getOrPut(folder) { ScrollState(0) },
                avatarLoader = avatarLoader,
                uriHandler = uriHandler,
                onBack = back,
                onOpenFolder = { folderStack.add(it) }
            )
        },
        modifier = modifier.fillMaxSize()
    )
}

/** Корень раздела: официальные сервисы + папка «Другие ресурсы». */
@Composable
private fun ResourcesRootContent(
    scrollState: androidx.compose.foundation.ScrollState,
    uriHandler: UriHandler,
    onBack: () -> Unit,
    onOpenFolder: (ResourceFolder) -> Unit
) {
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
                    text = "Ресурсы университета",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlitchText(
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
            FolderCard(folder = OTHER_RESOURCES_ROOT) { onOpenFolder(OTHER_RESOURCES_ROOT) }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/** Слой папки: её подпапки и ссылки, «назад» снимает слой. */
@Composable
private fun FolderContentScreen(
    folder: ResourceFolder,
    scrollState: ScrollState,
    avatarLoader: VkAvatarLoader,
    uriHandler: UriHandler,
    onBack: () -> Unit,
    onOpenFolder: (ResourceFolder) -> Unit
) {
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
                    text = folder.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (folder.children.isEmpty()) {
                EmptyFolderPlaceholder()
            } else {
                folder.children.forEach { child ->
                    when (child) {
                        is ResourceFolder -> FolderCard(folder = child) { onOpenFolder(child) }
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
                        .background(res.iconBackground ?: res.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val logo = res.logo
                    if (logo != null) {
                        Image(
                            painter = painterResource(logo),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Icon(
                            imageVector = res.icon,
                            contentDescription = null,
                            tint = res.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    GlitchText(
                        text = res.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    GlitchText(
                        text = displayWithoutProtocol(res.url),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlitchText(
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
                GlitchText(
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
            GlitchText(
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
                    GlitchText(
                        text = link.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    GlitchText(
                        text = link.url?.let(::displayWithoutProtocol) ?: "В разработке",
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

            GlitchText(
                text = link.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (link.url != null) {
                ResourceLinkButton(url = link.url, uriHandler = uriHandler)
                link.secondaryUrl?.let { secondaryUrl ->
                    Spacer(modifier = Modifier.height(8.dp))
                    ResourceLinkButton(url = secondaryUrl, uriHandler = uriHandler)
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
                    GlitchText(
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
private fun ResourceLinkButton(url: String, uriHandler: UriHandler) {
    Button(
        onClick = { uriHandler.openUri(url) },
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
        GlitchText(
            text = linkButtonLabel(url),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
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
    } else if (link.logo != null) {
        val avatar = link.logoIsAvatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (avatar) Color.Transparent
                    else link.iconBackground ?: link.accentColor.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(link.logo),
                contentDescription = link.title,
                contentScale = if (avatar) ContentScale.Crop else ContentScale.Fit,
                modifier = if (avatar) Modifier.fillMaxSize() else Modifier.size(32.dp)
            )
        }
    } else if (link.icon != null) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(link.iconBackground ?: link.accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = link.icon,
                contentDescription = null,
                tint = link.accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    } else {
        SymbolAvatar(symbol = link.symbol, accentColor = link.accentColor)
    }
}

/** Заглушка аватара: иконка-эмодзи из данных (🐘, 🤖, 🐼 и т.п.). */
@Composable
private fun SymbolAvatar(symbol: String, accentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        GlitchText(
            text = symbol,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

/** Пустая папка: показываем сообщение о технических работах. */
@Composable
private fun EmptyFolderPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            GlitchText(
                text = "Ведутся технические работы",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** URL без протокола (https://, http://) и завершающего слэша — для отображения. */
private fun displayWithoutProtocol(url: String): String =
    url.removePrefix("https://").removePrefix("http://").removePrefix("https:").removePrefix("http:").removeSuffix("/")

private fun linkButtonLabel(url: String): String = when {
    url.contains("t.me") -> "Перейти в Telegram"
    url.contains("vk.me/join") -> "Присоединиться в ВК"
    url.contains("vk.") || url.contains("vk.ru") -> "Открыть страницу ВК"
    else -> "Перейти"
}