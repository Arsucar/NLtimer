package com.nltimer.core.tools.timing

import android.util.Log
import com.nltimer.core.data.database.dao.IconSearchMissDao
import com.nltimer.core.data.database.entity.IconSearchMissEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconSearchMissLog @Inject constructor(
    private val dao: IconSearchMissDao,
) {

    suspend fun record(query: String, library: String?) {
        val entity = IconSearchMissEntity(
            query = query,
            library = library ?: "all",
            timestamp = System.currentTimeMillis(),
        )
        dao.insert(entity)
        Log.i(TAG, "Icon search miss recorded: query=\"$query\", library=${entity.library}")
    }

    suspend fun getAll(): List<IconSearchMissEntity> = dao.getAll()

    suspend fun clear() {
        dao.clear()
    }

    suspend fun count(): Int = dao.count()

    companion object {
        private const val TAG = "IconSearchMiss"
    }
}
