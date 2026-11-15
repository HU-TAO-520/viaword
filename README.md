# Viaword

[English](#english) · [正體中文](#正體中文) · [简体中文](#简体中文)

## English

Viaword is an offline Android app for learning English vocabulary. Study new words by unit, listen to pronunciation, and review with meaning recall, spelling, and dictation. Review dates and progress are stored on the device, so you can resume a session later.

This demo uses Kotlin, Jetpack Compose, and Room. It includes two sample units with nine entries and pronunciation audio synthesized with [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M). Textbook scans and the full textbook word list are excluded. The app interface is in Simplified Chinese.

Open this repository in Android Studio, use JDK 17 or 21 for Gradle, and install Android SDK Platform 35 and Build-Tools 36.0.0. Run the app on a device or emulator with Android 8.0 (API 26) or newer.

From the repository root, run `./gradlew assembleDebug testDebugUnitTest` on macOS or Linux, or `.\gradlew.bat assembleDebug testDebugUnitTest` in Windows PowerShell. Find the debug APK at `app/build/outputs/apk/debug/app-debug.apk`.

## 正體中文

Viaword 是一款可離線使用的 Android 英語單字練習 App。你可以按單元學新詞、聽發音，並透過回想詞義、看詞義拼寫和聽寫來複習。複習日期與練習進度儲存在裝置上，下次可接著練習。

這個示範版使用 Kotlin、Jetpack Compose 和 Room，內附兩組共九筆的範例詞條，以及用 [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) 合成的發音。此版本不含教材掃描檔和整冊詞表。App 介面為簡體中文。

在 Android Studio 中開啟此儲存庫，將 Gradle 設為使用 JDK 17 或 21，並安裝 Android SDK Platform 35 和 Build-Tools 36.0.0。可在 Android 8.0（API 26）以上的實體裝置或模擬器執行。

在儲存庫根目錄，macOS 或 Linux 請執行 `./gradlew assembleDebug testDebugUnitTest`，Windows PowerShell 請執行 `.\gradlew.bat assembleDebug testDebugUnitTest`。偵錯 APK 位於 `app/build/outputs/apk/debug/app-debug.apk`。

## 简体中文

Viaword 是一款可以离线使用的 Android 英语单词练习应用。你可以按单元学新词、听发音，并通过回想词义、看义拼写和听写来复习。复习日期和练习进度保存在设备上，下次可以接着练习。

这个演示版使用 Kotlin、Jetpack Compose 和 Room，内置两组共九条示例词条，以及用 [Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) 合成的发音。此版本不含教材扫描件和整册词表。应用界面为简体中文。

在 Android Studio 中打开此仓库，将 Gradle 设为使用 JDK 17 或 21，并安装 Android SDK Platform 35 和 Build-Tools 36.0.0。可在 Android 8.0（API 26）以上的设备或模拟器中运行。

在仓库根目录，macOS 或 Linux 请运行 `./gradlew assembleDebug testDebugUnitTest`，Windows PowerShell 请运行 `.\gradlew.bat assembleDebug testDebugUnitTest`。调试 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。
