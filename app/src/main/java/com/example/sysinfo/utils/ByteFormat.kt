package com.example.sysinfo.utils

import kotlin.math.ln
import kotlin.math.pow

/**
 * 字节数格式化工具，单位 1024 进制（B/KB/MB/...）。
 */
object ByteFormat {

    /** 人类可读的字节大小，如 "1.5 MB"。 */
    fun bytes(bytes: Long): String {
        val unit = 1024.0
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / unit.pow(exp.toDouble()), pre)
    }

    /** 速率，如 "1.5 MB/s"。 */
    fun speed(bytesPerSec: Long): String = "${bytes(bytesPerSec)}/s"
}
