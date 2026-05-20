package com.nltimer.core.tools.match

import com.nltimer.core.data.model.Activity
import com.nltimer.core.data.model.Tag

object KeywordMatcher {
    fun matchTag(query: String, tags: List<Tag>): Tag? {
        val exact = tags.find { it.name.equals(query, ignoreCase = true) }
        if (exact != null) return exact
        return tags.find { tag ->
            val tokens = parseKeywords(tag.keywords)
            tokens.isNotEmpty() && tokens.any { it.equals(query, ignoreCase = true) }
        }
    }

    fun matchActivity(query: String, activities: List<Activity>): Activity? {
        val exact = activities.find { it.name.equals(query, ignoreCase = true) }
        if (exact != null) return exact
        return activities.find { activity ->
            val tokens = parseKeywords(activity.keywords)
            tokens.isNotEmpty() && tokens.any { it.equals(query, ignoreCase = true) }
        }
    }
}
