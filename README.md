# 系统信息查看器

## 一、目标与范围

- 面向 **Android 8.0（API 26）及以上**，提供实时与历史趋势的系统面板，覆盖：电池（电量、温度、电压、充放电状态）、网络（Wi‑Fi 状态/信号、移动数据状态/制式）、省电策略（电池节省模式、数据节省模式、热点开关）、设备与硬件（型号、CPU、内存、存储、传感器）、以及 **Cell Tower/小区信息（MCC/MNC/LAC/CID）** 的变更监听与记录。  
- 交互与体验：分页/标签展示、可下拉刷新、关键指标小部件、可选后台服务与省电优化、导出报告（文本/PDF）。  
- 合规与隐私：仅采集设备与连接信息，不收集用户隐私内容；动态申请敏感权限并给出明确用途说明；适配分区存储与后台限制。 

## 二、核心功能与技术方案

- 电池与温度  
  - 读取 **BatteryManager** 获取电量百分比、充电状态（充电/放电/满电/未充电）、健康度、技术类型、电压、温度（°C）；按需计算或显示估计续航（基于历史与场景的估算）。  
  - 温度以 °C 为主，可按用户偏好切换 °F；温度异常时触发阈值通知。  
- Wi‑Fi 与移动网络  
  - 使用 **ConnectivityManager** 判断网络是否连接、类型（Wi‑Fi/移动）、漫游状态；在 Wi‑Fi 下读取 **WifiManager** 的 SSID/BSSID、链路速度、信号强度（RSSI）等。  
  - 移动网络通过 **TelephonyManager** 获取网络类型（含 **LTE/NR**）、运营商信息；制式映射展示为 **2G/3G/4G/5G**。  
- 省电与热点策略  
  - 读取 **PowerManager** 的 **isPowerSaveMode**（电池节省模式）；读取 **ConnectivityManager.getRestrictBackgroundStatus**（数据节省/后台数据限制）；读取 **WifiManager.isWifiApEnabled**（Wi‑Fi 热点开关）。  
- 小区与信号变更监听  
  - 通过 **PhoneStateListener(LISTEN_CELL_INFO/LISTEN_SIGNAL_STRENGTHS)** 监听小区与信号变化；采集 **CellInfo** 中的 **MCC/MNC/LAC/CID** 等字段，记录时间戳并持久化，支持变更列表与导出。  
- 设备硬件与系统信息  
  - 读取 **Build**、**PackageManager**、**DisplayMetrics**、**SensorManager** 等获取设备型号、厂商、系统版本/API、屏幕分辨率与密度、传感器清单、已装应用概览等。  
- 性能与资源  
  - CPU/内存/存储：读取系统属性与文件（如 /proc/cpuinfo、/proc/meminfo、statfs）或使用系统 API 获取核心数、频率、内存与存储占用；历史曲线与峰值记录。  
- UI 与可视化  
  - Material 3 分页：仪表盘、电池、网络、省电、热点、Cell Tower、硬件、应用与权限、测试；图表展示历史趋势；可配置小部件与通知。 

## 三、权限与系统能力

- 权限清单（按功能分组，AndroidManifest.xml 声明 + 运行时申请）  
  - 网络与 Wi‑Fi：**ACCESS_NETWORK_STATE、ACCESS_WIFI_STATE、CHANGE_WIFI_STATE**（如需切换 Wi‑Fi）、**WIFI_SCAN**（如需扫描周边 AP）。  
  - 电话与蜂窝：**READ_PHONE_STATE、ACCESS_FINE_LOCATION、ACCESS_COARSE_LOCATION**（蜂窝与定位权限通常为高敏感，需充分说明用途）。  
  - 热点：**CHANGE_WIFI_STATE**（热点开关）。  
  - 电池与省电：**BATTERY_STATS**（无需权限即可读取大部分电池信息）、**REQUEST_IGNORE_BATTERY_OPTIMIZATIONS**（如需保活采集服务，建议用户自愿授权）。  
  - 存储与导出：**READ_EXTERNAL_STORAGE、WRITE_EXTERNAL_STORAGE**（分区存储下使用 MediaStore/SAF）。  
- 兼容性要点  
  - **Android 10+** 位置权限对蜂窝/附近 Wi‑Fi 扫描为硬性要求；**Android 12+** Wi‑Fi 扫描与连接权限拆分；**Android 9+** 后台限制需前台服务/豁免策略；**Android 14+** 后台启动 Activity 限制需用户手势触发。 

## 四、页面与交互设计

- 仪表盘  
  - 关键指标卡片：电量%、温度、网络类型/SSID、数据节省、电池节省、热点开关、CPU 负载、内存占用。  
- 电池  
  - 实时：电量条、状态、温度、电压、充放电电流/功率（如可取）、估计续航；历史曲线（温度/电量/功率）。  
- 网络  
  - Wi‑Fi：连接详情（SSID/BSSID、频段/信道、信号强度、链路速度）、扫描列表（可选）；  
  - 移动：网络类型（2G/3G/4G/5G）、运营商、漫游、信号强度（dBm/ASU）。  
- 省电与热点  
  - 省电：电池节省模式开关状态、数据节省状态、后台数据限制状态；  
  - 热点：热点开关、已连接客户端（如可取）、上行/下行速率（可选）。  
- Cell Tower  
  - 列表：MCC/MNC/LAC/CID、时间戳、运营商名称；变更差异高亮；地图/列表视图切换；导出 **CSV/PDF**。  
- 硬件与系统  
  - 设备与系统：型号/厂商、Android 版本/API、安全补丁、内核版本、构建指纹；  
  - 资源：CPU（核心/频率/厂商）、内存（总/可用/历史）、存储（总/已用/可用）；  
  - 传感器：名称/厂商/当前值；已装应用概览（名称/版本/安装时间）。  
- 设置与偏好  
  - 温度单位（°C/°F）、刷新间隔、后台采集策略、通知与告警阈值（温度/电量）、隐私与导出选项。 

## 五、实现步骤与注意事项

- 实现步骤  
  - 搭建工程（Kotlin/Java + Android Studio），按模块实现数据采集（Battery、Network、Cell、Hardware、System、App、Sensor）。  
  - 使用 **LiveData/Flow + Repository** 统一数据流；关键指标内存缓存 + 磁盘持久化（Room/对象映射）。  
  - 注册广播与监听：**ACTION_BATTERY_CHANGED**、**CONNECTIVITY_CHANGE**、**WifiManager.RSSI_CHANGED_ACTION**、**PhoneStateListener**；后台使用前台服务（通知展示）保证采集连续性。  
  - UI：分页导航、可刷新列表、图表（MPAndroidChart/AndroidPlot）、小部件（AppWidgetProvider）；适配深色主题与多语言。  
  - 测试：多机型/多系统版本覆盖；省电与后台场景验证；权限拒绝/回退路径；定位与蜂窝权限的合规提示。  
- 注意事项  
  - **Android 10+** 背景定位与 Wi‑Fi 扫描限制严格，尽量在用户交互场景触发或提供清晰引导；  
  - **Android 12+** Wi‑Fi 权限变更，需区分扫描与连接；  
  - **Android 13+** 通知权限、**Android 14+** 后台启动限制需处理；  
  - 不读取短信/通讯录/精确位置等敏感数据；导出文件加密与本地存储安全；  
  - 避免频繁唤醒 CPU/网络，采用节流与批处理降低耗电；  
  - 上架前完成 **Play 应用签名、隐私合规、权限说明、目标 API 级别** 等要求。

## 一、工程配置与依赖

- 新建 Android 工程（建议 **Kotlin** + **AGP 8.x**，最低 **API 24**，目标 **API 34**），模块名：**:app**
- build.gradle.kts（模块级）
- AndroidManifest.xml（仅声明必要权限与组件）

## 二、数据模型与仓库

- data/model（精简示例，按需扩展）
- data/repository（统一数据源）

## 三、后台监测与持久化

- data/db（Room 简表）
- service/MonitorService（前台服务 + 节流）
- ui/MonitorViewModel（LiveData 暴露）

## 四、界面与交互（Material 3

- ui/MonitorFragment（核心展示）

## ✅ 五、UI 界面布局（XML）与完整绑定逻辑

### 1. 主界面布局：`res/layout/frag_monitor.xml`

这是一个综合展示系统信息的 Fragment 布局，采用 `ConstraintLayout` 和多个信息区块，你可以根据需要进一步美化或使用 Jetpack Compose。

📄 **res/layout/frag_monitor.xml**

### 2. 补全 `FragMonitorBinding` 和绑定逻辑（续）

在前面 `MonitorFragment.kt` 的 `observe()` 方法中，我们已经定义了如 `bindBattery()`、`bindWifi()` 等方法，但缺少 `statusStr()` 的定义和 `fmt()` 工具函数。

🔧 在 `MonitorFragment.kt` 中补充如下工具函数：

### 3. 请求运行时权限（可选，但推荐）

在 `MonitorFragment` 的 `requestPermissions()` 方法中，我们可以请求敏感权限，例如位置（用于获取小区信息）、电话状态等。

🔒 补全 `requestPermissions()`：

## ✅ 六、应用入口 & Application 类

### 1. Application 类：`SysInfoApp.kt`

用于初始化全局 Repository 或 Service 等。

📄 **SysInfoApp.kt**
📄 **AndroidManifest.xml** 中注册 Application：

## ✅ 七、启动 Service（可选，用于后台监测）

如果你希望 App 在后台持续记录小区信息、电池温度变化等，可以手动或自动启动 `MonitorService`。

### 1. 启动服务的按钮（可选，在 UI 中添加）

在 `frag_monitor.xml` 中添加一个按钮：

然后在 `MonitorFragment.kt` 的 `onViewCreated()` 中绑定点击事件：

🔒 注意：从 Android 8.0 开始，后台 Service 必须使用 `startForegroundService()` 并在 `onCreate()` 中调用 `startForeground()`，否则会引发 ANR 或崩溃。

我们已在 `MonitorService` 中实现了 `startForeground(1, ...)`，符合要求。

## ✅ 八、可选增强功能（供后续迭代）

| 功能 | 描述 | 是否已完成 |
|------|------|-------------|
| 📊 图表展示（电池温度/内存趋势） | 使用 MPAndroidChart 展示历史数值 | ❌ 待扩展 |
| 📁 导出数据（小区/日志） | 导出 CellTower 或日志到 CSV/文件 | ❌ 待扩展 |
| ⚙️ 设置页面 | 用户可选择刷新频率、是否启用后台服务等 | ❌ 待扩展 |
| 🔔 通知提醒 | 低电量、高温、信号弱等预警 | ❌ 待扩展 |
| 🤖 Widget 小部件 | 桌面小组件显示关键信息 | ❌ 待扩展 |
| 📲 快捷方式 | 一键清理/重启服务 | ❌ 待扩展 |

## ✅ 九、总结：你现在已经拥有

✅ 一个完整的 **Android 系统信息查看器 App**，包含以下功能模块：

| 模块 | 功能 | 是否实现 |
|------|------|----------|
| 🔋 电池 | 电量、温度、电压、充电状态、省电模式 | ✅ |
| 📶 Wi-Fi | 连接状态、SSID、信号强度、频段 | ✅ |
| 📡 移动网络 | 连接状态、网络类型、运营商、漫游 | ✅ |
| 🔥 热点 | Wi-Fi 热点开关状态 | ✅ |
| 📡 小区 | Cell Tower 信息（MCC/MNC/LAC/CID） | ✅ |
| 📱 硬件 | 品牌、型号、设备、主板、Android ID | ✅ |
| 💾 内存 | 总内存、可用内存、低内存状态 | ✅ |
| ⚙️ CPU | 核心数、频率、架构、ABI | ✅ |
| 🤖 系统 | Android 版本、SDK、安全补丁 | ✅ |
| 📊 UI | Material 3、实时刷新、下拉刷新 | ✅ |
| 🔒 权限 | 动态申请位置/电话等敏感权限 | ✅ |
| 🛠 后台服务 | 可选的后台监测服务（CellTower 记录） | ✅ |

## ✅ 下一步建议

1. **运行 & 测试**：在真机（尤其是不同品牌/Android 版本）上测试各项信息是否准确显示。
2. **优化 UI**：使用 `Jetpack Compose` 或更美观的卡片布局。
3. **添加图表**：集成 `MPAndroidChart` 显示电池温度/内存变化曲线。
4. **数据持久化 & 导出**：将小区历史保存到数据库并支持导出。
5. **后台服务优化**：使用 WorkManager 定期采集，或提供用户可控的后台策略。

---

如你需要，我可以继续帮你实现：

- 图表展示模块（MPAndroidChart）
- 数据导出功能（CSV/JSON）
- 后台 Service 优化（WorkManager）
- Jetpack Compose 版本重构

请告诉我你接下来的需求！ 😊