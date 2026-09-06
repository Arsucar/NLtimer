package com.nltimer.feature.stats.ui.component

internal data class GridDropItem(
    val index: Int,
    val offsetX: Int,
    val offsetY: Int,
    val width: Int,
    val height: Int,
    val colSpan: Int,
)

internal fun computeGridDropIndex(
    pointerX: Float,
    pointerY: Float,
    items: List<GridDropItem>,
    itemCount: Int,
    columns: Int = GridColumns,
): Int {
    if (itemCount <= 0) return 0
    if (items.isEmpty()) return itemCount

    val hit = items.firstOrNull { item ->
        pointerX >= item.offsetX &&
            pointerX < item.offsetX + item.width &&
            pointerY >= item.offsetY &&
            pointerY < item.offsetY + item.height
    }
    if (hit != null) {
        val after = if (hit.colSpan >= columns) {
            pointerY >= hit.offsetY + hit.height / 2f
        } else {
            pointerX >= hit.offsetX + hit.width / 2f
        }
        val insertBefore = if (after) hit.index + 1 else hit.index
        return insertBefore.coerceIn(0, itemCount)
    }

    val lastAbove = items
        .filter { it.offsetY + it.height / 2f <= pointerY }
        .maxByOrNull { it.index }
    if (lastAbove == null) {
        return items.minOf { it.index }.coerceIn(0, itemCount)
    }
    return (lastAbove.index + 1).coerceIn(0, itemCount)
}
