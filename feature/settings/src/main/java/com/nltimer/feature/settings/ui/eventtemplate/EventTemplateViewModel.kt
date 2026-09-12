package com.nltimer.feature.settings.ui.eventtemplate

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nltimer.core.data.model.EventFieldType
import com.nltimer.core.data.model.EventTemplate
import com.nltimer.core.data.model.EventTemplateField
import com.nltimer.core.data.repository.EventTemplateRepository
import com.nltimer.core.data.usecase.CreateOrUpdateTemplateUseCase
import com.nltimer.core.data.usecase.DeleteTemplateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STATE_TIMEOUT_MS = 5_000L

/** 字段草稿：uid 供列表 key / 编辑定位使用，保存时映射回 EventTemplateField */
@Immutable
data class EventTemplateFieldDraft(
    val uid: Int,
    val fieldId: Long = 0,
    val name: String = "",
    val type: EventFieldType = EventFieldType.TEXT,
    /** 单选字段的选项草稿整段文本（逗号/换行分隔，实时可编辑） */
    val optionsText: String = "",
    val isExpanded: Boolean = false,
)

/**
 * 打点模板编辑态（VM 托管，配置变更/进程内返回不丢失草稿）
 * editingId = null 表示新建模板
 */
@Immutable
data class EventTemplateEditState(
    val editingId: Long? = null,
    val name: String = "",
    val fields: List<EventTemplateFieldDraft> = emptyList(),
    val isSaving: Boolean = false,
    val validationError: String? = null,
)

@HiltViewModel
class EventTemplateViewModel @Inject constructor(
    private val eventTemplateRepository: EventTemplateRepository,
    private val createOrUpdateTemplateUseCase: CreateOrUpdateTemplateUseCase,
    private val deleteTemplateUseCase: DeleteTemplateUseCase,
) : ViewModel() {

    val templates: StateFlow<List<EventTemplate>> = eventTemplateRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS), emptyList())

    /** 模板字段数（列表页「N 字段」摘要），key = templateId */
    @OptIn(ExperimentalCoroutinesApi::class)
    val fieldCounts: StateFlow<Map<Long, Int>> = templates
        .flatMapLatest { list ->
            flow {
                if (list.isEmpty()) {
                    emit(emptyMap())
                } else {
                    val ids = list.map { it.id }
                    val map = eventTemplateRepository.getFieldsForTemplatesSync(ids)
                        .mapValues { (_, fields) -> fields.size }
                    // 保证每个模板都有条目，计数 0 也显式显示
                    emit(list.associate { it.id to (map[it.id] ?: 0) })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS), emptyMap())

    private val _editState = MutableStateFlow(EventTemplateEditState())
    val editState: StateFlow<EventTemplateEditState> = _editState.asStateFlow()

    private var draftUidSeed = 0

    /** 进入编辑页时准备草稿：加载已有模板字段或清空新建态 */
    fun prepareEdit(templateId: Long?) {
        viewModelScope.launch {
            if (_editState.value.isSaving) return@launch
            val editingId = _editState.value.editingId
            if (templateId == null) {
                if (editingId != null || _editState.value.fields.isNotEmpty()) {
                    _editState.value = EventTemplateEditState()
                }
                return@launch
            }
            if (editingId == templateId && _editState.value.fields.isNotEmpty()) {
                // 已装载同一模板（进程内返回），保留草稿
                return@launch
            }
            val template = eventTemplateRepository.getTemplateById(templateId) ?: return@launch
            val fields = eventTemplateRepository.getFieldsByTemplateSync(templateId)
            _editState.value = EventTemplateEditState(
                editingId = template.id,
                name = template.name,
                fields = fields.map { field ->
                    draftUidSeed += 1
                    EventTemplateFieldDraft(
                        uid = draftUidSeed,
                        fieldId = field.id,
                        name = field.name,
                        type = field.type,
                        optionsText = field.options.joinToString("，"),
                        isExpanded = false,
                    )
                },
            )
        }
    }

    fun updateEditName(name: String) {
        _editState.update { it.copy(name = name, validationError = null) }
    }

    fun addField() {
        draftUidSeed += 1
        _editState.update { state ->
            state.copy(
                fields = state.fields + EventTemplateFieldDraft(uid = draftUidSeed),
                validationError = null,
            )
        }
    }

    fun removeField(uid: Int) {
        _editState.update { state ->
            state.copy(fields = state.fields.filterNot { it.uid == uid })
        }
    }

    fun toggleFieldExpand(uid: Int) {
        _editState.update { state ->
            state.copy(
                fields = state.fields.map {
                    if (it.uid == uid) it.copy(isExpanded = !it.isExpanded) else it
                }
            )
        }
    }

    fun updateFieldDraft(uid: Int, transform: (EventTemplateFieldDraft) -> EventTemplateFieldDraft) {
        _editState.update { state ->
            state.copy(
                fields = state.fields.map { if (it.uid == uid) transform(it) else it },
                validationError = null,
            )
        }
    }

    fun moveField(uid: Int, up: Boolean) {
        _editState.update { state ->
            val index = state.fields.indexOfFirst { it.uid == uid }
            if (index < 0) return@update state
            val target = if (up) index - 1 else index + 1
            if (target < 0 || target >= state.fields.size) return@update state
            val mutable = state.fields.toMutableList().apply {
                val moved = removeAt(index)
                add(target, moved)
            }
            state.copy(fields = mutable)
        }
    }

    /** 保存（新建或更新）；result 交由 UI 决定导航返回 / 错误提示 */
    fun saveTemplate(onResult: (CreateOrUpdateTemplateUseCase.Result) -> Unit) {
        val state = _editState.value
        if (state.isSaving) return
        viewModelScope.launch {
            _editState.update { it.copy(isSaving = true, validationError = null) }
            val fields = state.fields.map { draft ->
                EventTemplateField(
                    // 既有字段保留原 id（差量替换保留其事件值）；新字段 id=0 走 insert
                    id = draft.fieldId,
                    templateId = 0,
                    name = draft.name,
                    type = draft.type,
                    options = parseOptions(draft.type, draft.optionsText),
                )
            }
            val result = createOrUpdateTemplateUseCase(
                editTemplateId = state.editingId,
                name = state.name,
                fields = fields,
                // 编辑页不维护绑定；保留模板已有的标签绑定不被清掉
                tagIds = state.editingId?.let { eventTemplateRepository.getTagIdsForTemplateSync(it) } ?: emptyList(),
            )
            when (result) {
                is CreateOrUpdateTemplateUseCase.Result.ValidationError ->
                    _editState.update { it.copy(isSaving = false, validationError = result.message) }
                // 成功后重置会话：避免同 id 重进时沿用 id=0 的旧草稿，
                // 二次保存把既有字段整体重建导致存量事件值级联清空
                is CreateOrUpdateTemplateUseCase.Result.Success ->
                    _editState.value = EventTemplateEditState()
            }
            onResult(result)
        }
    }

    /** 删除模板（UI 已先弹确认说明存量事件级联影响） */
    fun deleteTemplate(templateId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            deleteTemplateUseCase(templateId)
            onDeleted()
        }
    }

    private fun parseOptions(type: EventFieldType, raw: String): List<String> {
        if (type != EventFieldType.SELECT) return emptyList()
        return raw.split("，", ",", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
