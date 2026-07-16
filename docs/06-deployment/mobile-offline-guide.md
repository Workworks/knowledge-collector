# Android 离线包

移动端是只读离线资料库，不运行 Spring Boot、不采集、不调用 Ollama。文章和已有 AI 结果在打包时写入 APK，首次启动导入 Android 内置 SQLite。

`AndroidManifest.xml` 故意不声明 `android.permission.INTERNET`，因此 APK 运行时不能访问外部网络。

先启动桌面端，然后执行：

```powershell
.\scripts\package-mobile-offline.ps1 -BaseUrl http://127.0.0.1:8080 -MaxArticles 500
```

仅导出数据：

```powershell
.\scripts\package-mobile-offline.ps1 -SkipBuild
```

构建需要 Android Studio/Android SDK、JDK 11 或更高版本和可执行的 Gradle 7.6。也可用 Android Studio 打开 `mobile-offline` 后执行 Assemble。

导出数据位于 `mobile-offline/app/src/main/assets/articles.json`，调试 APK 位于 `mobile-offline/app/build/outputs/apk/debug/app-debug.apk`。

离线包只包含清洗后的纯文本正文、摘要和已有 AI 结果，不包含 Cookie、Token、采集配置或凭据。更新资料需要重新打包。
