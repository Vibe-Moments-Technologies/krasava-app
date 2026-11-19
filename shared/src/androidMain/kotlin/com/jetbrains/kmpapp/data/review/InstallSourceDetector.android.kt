package com.jetbrains.kmpapp.data.review

import com.jetbrains.kmpapp.data.storage.AndroidContextProvider

/**
 * Android: читаем установщика пакета.
 *
 * `getInstallSourceInfo` доступен с API 30; на более старых — устаревший
 * `getInstallerPackageName`. Оба вызова могут бросить (пакет не найден,
 * SELinux на некоторых прошивках) — тогда считаем установку ручной.
 */
actual fun detectInstallSource(): InstallSource {
    val ctx = AndroidContextProvider.context ?: return InstallSource.SIDELOAD
    val pm = ctx.packageManager
    val pkg = ctx.packageName
    val installer = if (android.os.Build.VERSION.SDK_INT >= 30) {
        runCatching { pm.getInstallSourceInfo(pkg).installingPackageName }.getOrNull()
    } else {
        @Suppress("DEPRECATION")
        runCatching { pm.getInstallerPackageName(pkg) }.getOrNull()
    }
    return installSourceFromInstallerPackage(installer)
}