package com.nltimer.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * ActivityTagBindingEntity 活动-标签关联实体
 * 多对多关联表，连接 activities 与 tags
 *
 * @property source 绑定方向：'tag' 表示从标签管理侧发起（标签→活动），
 *   'activity' 表示从活动管理侧发起（活动→标签）。两侧独立，互不影响。
 */
@Entity(
    tableName = "activity_tag_binding",
    primaryKeys = ["activityId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("activityId"), Index("tagId")],
)
data class ActivityTagBindingEntity(
    val activityId: Long,
    val tagId: Long,
    val source: String = "tag",
)
