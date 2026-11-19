package com.jetbrains.kmpapp.data.review

/**
 * iOS: приложение в App Store не публиковалось — сборки ставятся через
 * sideload IPA, оставить отзыв в магазине нельзя. Просим только звезду.
 */
actual fun detectInstallSource(): InstallSource = InstallSource.SIDELOAD