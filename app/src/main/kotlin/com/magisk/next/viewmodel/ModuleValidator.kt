package com.magisk.next.viewmodel

import android.content.Context
import com.magisk.next.R

class ModuleValidator(
    private val moduleId: String,
    private val moduleName: String,
    private val moduleVersion: String,
    private val moduleDescription: String,
    private val customizeScript: String,
    private val moduleFilesCount: Int
) {
    fun validate(context: Context): List<ValidationItem> {
        val items = mutableListOf<ValidationItem>()
        if (moduleId.isBlank() || !moduleId.matches(Regex("^[a-zA-Z0-9._-]+$")))
            items.add(ValidationItem("error", context.getString(R.string.validation_error_id)))
        else items.add(ValidationItem("success", context.getString(R.string.validation_success_id)))
        if (moduleName.isBlank()) items.add(ValidationItem("error", context.getString(R.string.validation_error_name)))
        if (moduleVersion.isBlank() || !moduleVersion.matches(Regex("^[a-zA-Z0-9.\\-_]+$")))
            items.add(ValidationItem("error", context.getString(R.string.validation_error_version)))
        if (moduleDescription.isBlank()) items.add(ValidationItem("warning", context.getString(R.string.validation_warning_description)))
        if (customizeScript.isBlank()) items.add(ValidationItem("warning", context.getString(R.string.validation_warning_customize_empty)))
        else if (!customizeScript.startsWith("#!/")) items.add(ValidationItem("warning", context.getString(R.string.validation_warning_no_shebang)))
        if (moduleFilesCount == 0) items.add(ValidationItem("info", context.getString(R.string.validation_info_no_files)))
        else items.add(ValidationItem("success", context.getString(R.string.validation_success_files, moduleFilesCount)))
        return items
    }
}