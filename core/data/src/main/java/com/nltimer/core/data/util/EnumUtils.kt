package com.nltimer.core.data.util

inline fun <reified T : Enum<T>> safeValueOf(name: String, default: T): T {
    return try {
        enumValueOf<T>(name)
    } catch (_: IllegalArgumentException) {
        default
    }
}
