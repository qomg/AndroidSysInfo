# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ---------------------------------------------------------------------------
# 保留源文件与行号，便于崩溃堆栈还原
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Kotlin
# ---------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-dontwarn kotlin.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** getInstance(android.content.Context);
}
# 保留 Entity 字段，Room 通过反射读写
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
}
# 本应用 Room 实体与 DAO
-keep class com.example.sysinfo.data.model.CellTower { *; }
-keep interface com.example.sysinfo.data.db.CellTowerDao { *; }
-keep class com.example.sysinfo.data.db.AppDatabase { *; }
-keep class com.example.sysinfo.data.db.AppDatabase$Companion { *; }

# ---------------------------------------------------------------------------
# WorkManager (Worker 通过反射实例化)
# ---------------------------------------------------------------------------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.example.sysinfo.worker.CellAndBatteryMonitorWorker { *; }
-keep class com.example.sysinfo.worker.CellAndBatteryMonitorWorkerKt { *; }

# ---------------------------------------------------------------------------
# Android 组件（避免反射 / 序列化导致问题）
# ---------------------------------------------------------------------------
-keep class com.example.sysinfo.SysInfoApp { *; }
-keep class com.example.sysinfo.ui.MainActivity { *; }
-keep class com.example.sysinfo.ui.UsageActivity { *; }
-keep class com.example.sysinfo.service.MonitorService { *; }
-keep class com.example.sysinfo.ui.PermissionFragment { *; }
-keep class com.example.sysinfo.ui.MonitorFragment { *; }

# ---------------------------------------------------------------------------
# ViewModel / LiveData（组件通过反射创建 ViewModel）
# ---------------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}
-keep class com.example.sysinfo.ui.MonitorViewModel { *; }

# ---------------------------------------------------------------------------
# 数据模型（用于序列化 / 数据库 / 跨模块时保留）
# ---------------------------------------------------------------------------
-keep class com.example.sysinfo.data.model.BatteryState { *; }
-keep class com.example.sysinfo.data.model.WifiState { *; }
-keep class com.example.sysinfo.data.model.MobileState { *; }
-keep class com.example.sysinfo.data.model.HardwareInfo { *; }
-keep class com.example.sysinfo.data.model.MemoryState { *; }
-keep class com.example.sysinfo.data.model.CpuState { *; }
-keep class com.example.sysinfo.data.model.SystemInfo { *; }
-keep class com.example.sysinfo.usage.AppUsageAnalysis { *; }
-keep class com.example.sysinfo.usage.UsageFrequency { *; }

# ---------------------------------------------------------------------------
# 通用 Android / 第三方
# ---------------------------------------------------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# R8 对枚举的优化
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}
