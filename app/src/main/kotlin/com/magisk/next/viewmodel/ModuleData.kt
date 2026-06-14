package com.magisk.next.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.magisk.next.model.ModuleFile

class ModuleData {
    var moduleId by mutableStateOf("")
    var moduleName by mutableStateOf("")
    var moduleVersion by mutableStateOf("")
    var moduleVersionCode by mutableStateOf("")
    var moduleAuthor by mutableStateOf("")
    var moduleLink by mutableStateOf("")
    var moduleDescription by mutableStateOf("")
    var moduleChangelog by mutableStateOf("")

    var customizeScript by mutableStateOf("")
    var serviceScript by mutableStateOf("")
    var postFsScript by mutableStateOf("")
    var updateBinaryType by mutableStateOf("symlink")
    var updateBinaryCustom by mutableStateOf("")

    var minMagisk by mutableStateOf("20400")
    var systemless by mutableStateOf(true)
    var needsystem by mutableStateOf(false)
    var skipMount by mutableStateOf(false)
    var recoveryMode by mutableStateOf(false)
    var replaceFiles by mutableStateOf("")
    var sepolicyRules by mutableStateOf("")
    var verifyKey by mutableStateOf("")

    // [+] Автообновление (updateJson)
    var updateJsonEnabled by mutableStateOf(false)
    var updateJsonUrl by mutableStateOf("")  // URL до update.json (идёт в module.prop)
    var updateZipUrl by mutableStateOf("")   // URL до ZIP релиза (идёт в update.json)

    val moduleFiles = mutableStateListOf<ModuleFile>()
}