package com.nltimer.core.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class ArchiveGroup<T>(
    val name: String,
    val items: List<T>,
)

fun <T> groupArchivedItems(
    items: List<T>,
    groupName: (T) -> String,
    archivedAt: (T) -> Long?,
    itemName: (T) -> String,
    uncategorizedName: String,
): List<ArchiveGroup<T>> {
    if (items.isEmpty()) return emptyList()
    val grouped = items.groupBy(groupName)
    fun sorted(list: List<T>): List<T> =
        list.sortedWith(
            compareByDescending<T> { archivedAt(it) ?: Long.MIN_VALUE }
                .thenBy(itemName),
        )
    val uncategorizedItems = grouped[uncategorizedName]
    val others = grouped.filterKeys { it != uncategorizedName }
        .map { (name, list) -> name to sorted(list) }
        .sortedByDescending { (_, list) ->
            list.maxOf { archivedAt(it) ?: Long.MIN_VALUE }
        }
    return buildList {
        if (uncategorizedItems != null) {
            add(ArchiveGroup(uncategorizedName, sorted(uncategorizedItems)))
        }
        others.forEach { (name, list) -> add(ArchiveGroup(name, list)) }
    }
}
