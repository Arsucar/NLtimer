package com.nltimer.core.designsystem.form

object ActivityFormSpecs {
    val createActivity = FormSpec(
        title = "增加活动",
        submitLabel = "增加活动",
        sections = listOf(
            FormSection(
                rows = listOf(
                    FormRow.IconColor(iconKey = "icon", colorKey = "color", initialEmoji = "📖"),
                ),
            ),
            FormSection(
                rows = listOf(
                    FormRow.TextInput(key = "name", label = "名称", placeholder = "请输入"),
                    FormRow.TextInput(key = "keywords", label = "关键词", placeholder = "多个关键词用逗号分隔"),
                ),
            ),
            FormSection(
                rows = listOf(
                    FormRow.LabelAction(key = "tags", label = "关联标签", actionText = "+ 增加"),
                ),
            ),
            FormSection(
                rows = listOf(
                    FormRow.LabelAction(key = "category", label = "所属分类", actionText = "未分类"),
                ),
            ),
        ),
    )

    fun editActivity() = FormSpec(
        title = "编辑活动",
        submitLabel = "保存",
        sections = createActivity.sections,
    )

    val createTag = FormSpec(
        title = "新增标签",
        submitLabel = "新增标签",
        sections = listOf(
            FormSection(
                rows = listOf(
                    FormRow.IconColor(iconKey = "icon", colorKey = "color", initialEmoji = "hi:Tag01"),
                ),
            ),
            FormSection(
                rows = listOf(
                    FormRow.TextInput(key = "name", label = "名称", placeholder = "请输入标签名"),
                    FormRow.TextInput(key = "keywords", label = "关键词", placeholder = "多个关键词用逗号分隔"),
                    FormRow.NumberInput(key = "priority", label = "优先级", initialValue = 0, range = 0..99),
                ),
            ),
            FormSection(
                rows = listOf(
                    FormRow.LabelAction(key = "activities", label = "关联活动", actionText = "+ 增加"),
                ),
            ),
            FormSection(
                rows = listOf(
                    FormRow.LabelAction(key = "category", label = "所属分类", actionText = "未分类"),
                ),
            ),
        ),
    )

    fun editTag() = FormSpec(
        title = "编辑标签",
        submitLabel = "保存",
        sections = createTag.sections,
    )
}
