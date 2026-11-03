package com.example.model

/**
 * Clean data representation of an on-screen Accessibility node.
 * Used for AI screen comprehension and gesture targeting.
 */
data class UiNodeInfo(
    val id: Int,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val centerX: Int,
    val centerY: Int,
    val width: Int,
    val height: Int,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isEnabled: Boolean,
    val depth: Int
) {
    /**
     * Compact prompt summary. The actual text entered into editable fields is never sent
     * to Gemini; only a generic field label/accessibility description is included.
     */
    fun toPromptSummary(): String {
        val label = if (isEditable) {
            if (!viewId.isNullOrBlank()) {
                "editable field (id: ${viewId.substringAfterLast('/')})"
            } else {
                "editable text field"
            }
        } else {
            when {
                !text.isNullOrBlank() && !contentDescription.isNullOrBlank() ->
                    "\"$text\" (desc: \"$contentDescription\")"
                !text.isNullOrBlank() -> "\"$text\""
                !contentDescription.isNullOrBlank() -> "desc: \"$contentDescription\""
                !viewId.isNullOrBlank() -> "id: ${viewId.substringAfterLast('/')}"
                else -> className.substringAfterLast('.')
            }
        }

        val flags = buildList {
            if (isClickable) add("clickable")
            if (isEditable) add("editable")
            if (isScrollable) add("scrollable")
            if (isCheckable) add(if (isChecked) "checked" else "unchecked")
        }.joinToString(",")

        val flagsStr = if (flags.isNotEmpty()) " [$flags]" else ""
        return "Node #$id: $label at center=($centerX, $centerY), bounds=[$left, $top, $right, $bottom]$flagsStr"
    }
}
