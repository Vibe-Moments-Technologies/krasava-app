// swift-tools-version: 5.9
import PackageDescription

// Пакет существует только ради unit-тестов логики виджета: XCTest нельзя
// запустить иначе, а чистые исходники (модели + движок выбора пары) лежат
// в iosApp/KrasavaWidget/Logic — там же, откуда их компилирует таргет
// расширения. Содержимое не дублируется, path указывает на общий каталог.
let package = Package(
    name: "WidgetKitLogic",
    platforms: [.macOS(.v13), .iOS(.v17)],
    products: [
        .library(name: "WidgetKitLogic", targets: ["WidgetKitLogic"])
    ],
    targets: [
        .target(
            name: "WidgetKitLogic",
            path: "../KrasavaWidget/Logic"
        ),
        .testTarget(
            name: "WidgetKitLogicTests",
            dependencies: ["WidgetKitLogic"],
            path: "Tests/WidgetKitLogicTests"
        )
    ]
)