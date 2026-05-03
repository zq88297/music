# Rear Lyrics

一个原生 Android 原型应用：监听当前手机音乐播放器的媒体会话，尽量抓取歌词/文案，并把内容投到系统暴露出来的副屏上。针对“小米 17 Pro 背屏”这个目标设备，当前版本采用的是公开 Android API 能做到的最稳妥方案。

## 当前能力

- 通过 `NotificationListenerService` + `MediaSessionManager.getActiveSessions(...)` 读取当前活跃播放器的标题、歌手和可能存在的歌词字段。
- 优先把歌词显示到系统识别为 `FLAG_PRESENTATION` 的 secondary display。
- 如果设备没有把背屏开放成 Android 副屏，主界面里会显示同样的背屏预览，便于先验证歌词状态流与 UI。
- 提供手动粘贴歌词兜底，因为绝大多数播放器不会通过公开 API 暴露逐字滚动歌词。

## 重要限制

1. 公开 Android API 能访问“正在播放的信息”，但不能保证拿到所有播放器的实时歌词。
2. 小米背屏能否被第三方 App 当作 secondary display 使用，取决于 MIUI/HyperOS 是否公开对应显示设备。
3. 如果真机上检测不到副屏，就需要进一步针对小米的私有能力做适配；这一步通常需要 OEM 文档、系统签名能力或逆向验证。

## 打开方式

1. 用 Android Studio 打开当前目录。
2. 让 Gradle 同步下载依赖。
3. 安装到目标手机。
4. 在应用内点击“开启通知监听”，为 `Rear Lyrics` 打开通知读取权限。
5. 开始播放音乐，再点击“投到背屏”。

## 建议的下一步真机适配

- 在目标设备上打印 `DisplayManager.displays` 的名称、id、flags，确认背屏是否被系统公开。
- 针对你常用的播放器，抓取 `MediaMetadata`/`Notification.extras` 里的真实字段，补充专用歌词提取规则。
- 如果小米系统提供私有背屏 SDK，再把 `RearDisplayController` 替换成 OEM 通道。

## 现在如何排查

- 看“显示设备”调试块：如果只有默认屏，说明系统没有把背屏公开成可投的 Android 副屏。
- 看“歌词候选”调试块：如果为空，说明当前播放器没有通过公开媒体接口提供歌词。
- 看 `MediaMetadata` 和 `Controller Extras`：这里会列出播放器真实暴露的字符串字段，后续可以按字段名做定向提取。
