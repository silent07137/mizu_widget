# 开发说明

## 结构

- WidgetConfig：不可变配置及 JSON 序列化。
- ConfigStore：按 widget ID 存储、临时 pin 草稿、URI 引用清理、ID 恢复映射。
- WidgetRenderer / Geometry：ImageDecoder 采样、EXIF 方向、透明通道、缩放、裁剪和内存上限。
- WidgetEngine：后台执行器、尺寸计算、原子缓存、RemoteViews 发布和独立 PendingIntent。
- MizuWidgetProvider：系统更新、尺寸改变、删除、恢复回调。
- ConfigureActivity / PinReceiver：配置、持久授权、桌面 pin 回调。
- MainActivity / ImageViewerActivity / AboutActivity：管理、图片预览和许可。
- androidTest：仅在测试 APK 中提供的合成图片及平台 Instrumentation。

## 生命周期

从桌面添加时，只有保存成功才返回 RESULT_OK；取消保留 RESULT_CANCELED。从应用添加时先保存唯一 token 的草稿，由桌面确认成功的 PendingIntent 回调接收实际 widget ID，再提交配置。未确认草稿 24 小时后在应用启动时清理。

草稿和所有实例共同引用 URI，同一张图片仍被其他实例引用时不会撤销权限。替换图片、取消配置、删除实例时只释放不再使用的授权。

配置页面旋转后从 savedInstanceState 恢复，预览任务按 generation 丢弃过期结果。图片解码、缓存和组件刷新在单一线程执行，避免实例写入互相交错。

定期更新间隔为 0；组件依靠系统更新、尺寸改变、配置保存及用户刷新进行重绘。重启时由系统 App Widget 更新机制恢复，无 BOOT_COMPLETED 权限或常驻服务。

## 初版边界

GIF 和其他可解码动图只显示静态帧，没有动图调度器、1/2/5 FPS 设置。不能把普通 GIF View 放入 RemoteViews。后续播放需要单独设计与测试。

图片不透明度只作用于图片，不作用于用户选择的背景色。棋盘格仅是配置页面的透明预览，不会绘入桌面组件。

失效图片显示“轻点重新选择”，此修复点击覆盖用户原来设置的“不执行操作”。

当前缩放为居中操作，没有手动裁剪位置。超大图和极端长宽比图片以受限采样保证安全，可能损失局部精细度。
