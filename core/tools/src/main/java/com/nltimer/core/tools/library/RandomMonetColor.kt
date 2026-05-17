package com.nltimer.core.tools.library

import android.graphics.Color
import java.util.concurrent.ThreadLocalRandom

/**
 * 莫奈/MD3 风格随机色生成器
 *
 * 设计意图：让 AI 创建活动/标签时拿到视觉柔和、不刺眼的中和色。
 * 取 HSV：H 完全随机 [0,360)，S=0.55、V=0.62 固定，
 * 这一组合接近 Material You 主色板的中等饱和度色。
 *
 * 返回 ARGB 的 Long（高位 0xFF 完全不透明），与 Activity.color/Tag.color 字段类型对齐。
 */
internal object RandomMonetColor {
    private const val SATURATION = 0.55f
    private const val VALUE = 0.62f

    fun next(): Long {
        val hue = ThreadLocalRandom.current().nextFloat() * 360f
        val hsv = floatArrayOf(hue, SATURATION, VALUE)
        val argb = Color.HSVToColor(hsv).toLong() and 0xFFFFFFFFL
        return argb or 0xFF000000L
    }
}
