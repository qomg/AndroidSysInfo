package com.example.sysinfo.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 生成 Baseline Profile，覆盖应用启动与主界面关键路径。
 * 运行 :app:generateBaselineProfile 将结果写入 app/src/release/generated/baselineProfiles/
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = "com.example.sysinfo",
        includeInStartupProfile = true,
        profileBlock = {
            pressHome()
            startActivityAndWait()
        }
    )
}
