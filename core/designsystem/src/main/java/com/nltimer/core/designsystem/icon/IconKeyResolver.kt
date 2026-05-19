package com.nltimer.core.designsystem.icon

object IconKeyResolver {
    private const val PREFIX_MI = "mi:"
    private const val PREFIX_HI = "hi:"
    private val NAME_PATTERN = Regex("^[A-Za-z0-9_]+$")

    fun isMaterialIcon(iconKey: String): Boolean = iconKey.startsWith(PREFIX_MI)

    fun isHugeIcon(iconKey: String): Boolean = iconKey.startsWith(PREFIX_HI)

    fun isMaterialIconOrNull(iconKey: String?): Boolean =
        iconKey != null && isMaterialIcon(iconKey)

    fun isHugeIconOrNull(iconKey: String?): Boolean =
        iconKey != null && isHugeIcon(iconKey)

    fun parseMaterialIcon(iconKey: String): Pair<String, String>? {
        if (!isMaterialIcon(iconKey)) return null
        val parts = iconKey.removePrefix(PREFIX_MI).split(":", limit = 2)
        if (parts.size != 2) return null
        val (style, name) = parts
        if (!NAME_PATTERN.matches(name)) return null
        return style to name
    }

    fun resolveImageVector(iconKey: String): androidx.compose.ui.graphics.vector.ImageVector? {
        if (isHugeIcon(iconKey)) {
            val name = iconKey.removePrefix(PREFIX_HI)
            return HugeIconCatalog.resolve(name)
        }
        val (style, name) = parseMaterialIcon(iconKey) ?: return null
        return MaterialIconCatalog.resolve(style, name)
    }

    fun iconKeyToDisplayText(iconKey: String?): String {
        if (iconKey == null) return ""
        if (isHugeIcon(iconKey)) {
            return iconKey.removePrefix(PREFIX_HI)
        }
        if (isMaterialIcon(iconKey)) {
            val (_, name) = parseMaterialIcon(iconKey) ?: return iconKey
            return name
        }
        return iconKey
    }
}
