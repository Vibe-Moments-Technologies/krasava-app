package com.jetbrains.kmpapp.data.theme

import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceStyle

actual fun isSystemDarkTheme(): Boolean =
    UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
