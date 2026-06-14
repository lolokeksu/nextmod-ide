package com.magisk.next.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.magisk.next.R
import com.magisk.next.ui.theme.TextPrimary
import com.magisk.next.ui.theme.TextSecondary
import com.magisk.next.ui.theme.TextFieldShape
import com.magisk.next.ui.theme.textFieldColors
import com.magisk.next.viewmodel.ModuleViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.magisk.next.ui.theme.BgPrimary

// [*] Regex создаются один раз, а не при каждом вызове idError()/versionError()
private val MODULE_ID_REGEX      = Regex("^[a-zA-Z0-9_]+$")
private val MODULE_VERSION_REGEX = Regex("^[a-zA-Z0-9.\\-_]+$")

@Composable
fun MetadataTab(viewModel: ModuleViewModel) {

    var idTouched by remember { mutableStateOf(false) }
    var nameTouched by remember { mutableStateOf(false) }
    var versionTouched by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    // [*] Job хранится в обычном объекте-обёртке — не вызывает рекомпозицию при смене Job
    val idDebounceRef      = remember { object { var job: Job? = null } }
    val versionDebounceRef = remember { object { var job: Job? = null } }

    fun idError(): Boolean {
        val v = viewModel.moduleId
        return v.isNotBlank() && !v.matches(MODULE_ID_REGEX)
    }
    fun nameError(): Boolean = false
    fun versionError(): Boolean {
        val v = viewModel.moduleVersion
        return v.isNotBlank() && !v.matches(MODULE_VERSION_REGEX)
    }

    fun onIdChange(newValue: String) {
        viewModel.moduleId = newValue
        idDebounceRef.job?.cancel()
        idDebounceRef.job = scope.launch {
            delay(300)
            idTouched = newValue.isNotEmpty()
        }
    }

    fun onNameChange(newValue: String) {
        viewModel.moduleName = newValue
        nameTouched = newValue.isNotEmpty()
    }

    fun onVersionChange(newValue: String) {
        viewModel.moduleVersion = newValue
        versionDebounceRef.job?.cancel()
        versionDebounceRef.job = scope.launch {
            delay(300)
            versionTouched = newValue.isNotEmpty()
        }
    }

    Box(
    modifier = Modifier
        .fillMaxSize()
        .background(BgPrimary)   // вместо MaterialTheme.colorScheme.background
) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // [*] AnimatedVisibility(visible = true) убран — добавлял overhead отслеживания
            //     анимации при каждой рекомпозиции без UX-ценности на экране редактирования
            OutlinedTextField(
            	shape = TextFieldShape,
                value = viewModel.moduleId,
                onValueChange = { onIdChange(it) },
                label = { Text(stringResource(R.string.label_module_id)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                isError = idTouched && idError(),
                supportingText = if (idTouched && idError()) {
                    { Text(stringResource(R.string.validation_error_id)) }
                } else null
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
            	shape = TextFieldShape,
                value = viewModel.moduleName,
                onValueChange = { onNameChange(it) },
                label = { Text(stringResource(R.string.label_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                isError = nameTouched && nameError(),
                supportingText = if (nameTouched && nameError()) {
                    { Text(stringResource(R.string.validation_error_name)) }
                } else null
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
            	shape = TextFieldShape,
                value = viewModel.moduleVersion,
                onValueChange = { onVersionChange(it) },
                label = { Text(stringResource(R.string.label_version)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                isError = versionTouched && versionError(),
                supportingText = if (versionTouched && versionError()) {
                    { Text(stringResource(R.string.validation_error_version)) }
                } else null
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
            	shape = TextFieldShape,
                value = viewModel.moduleVersionCode,
                onValueChange = { viewModel.moduleVersionCode = it },
                label = { Text(stringResource(R.string.label_version_code)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
            	shape = TextFieldShape,
                value = viewModel.moduleAuthor,
                onValueChange = { viewModel.moduleAuthor = it },
                label = { Text(stringResource(R.string.label_author)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
            	shape = TextFieldShape,
                value = viewModel.moduleLink,
                onValueChange = { viewModel.moduleLink = it },
                label = { Text(stringResource(R.string.label_link)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
            	shape = TextFieldShape,
                value = viewModel.moduleDescription,
                onValueChange = { viewModel.moduleDescription = it },
                label = { Text(stringResource(R.string.label_description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                colors = textFieldColors()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
            	shape = TextFieldShape,
                value = viewModel.moduleChangelog,
                onValueChange = { viewModel.moduleChangelog = it },
                label = { Text(stringResource(R.string.label_changelog)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                colors = textFieldColors()
            )
        }
    }
}