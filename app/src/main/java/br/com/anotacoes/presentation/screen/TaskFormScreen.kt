package br.com.anotacoes.presentation.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.anotacoes.domain.model.DayOfWeek
import br.com.anotacoes.presentation.RecurrenceTypeOption
import br.com.anotacoes.presentation.TaskFormIntent
import br.com.anotacoes.presentation.TaskFormViewModel
import coil.compose.AsyncImage
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_LABELS = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
    "Jul", "Ago", "Set", "Out", "Nov", "Dez")
private val PRESET_REMINDER_DAYS = listOf(1, 2, 3, 5, 7)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskFormScreen(
    viewModel: TaskFormViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }
    var showCustomReminderDialog by remember { mutableStateOf(false) }
    var customReminderText by remember { mutableStateOf("") }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    fun openFile(path: String) {
        val file = File(path)
        if (!file.exists()) return
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mimeType = context.contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Abrir com"))
        } catch (_: Exception) {
            // ActivityNotFoundException (no app handles this type) or
            // IllegalArgumentException (FileProvider path not covered) — ignore silently
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onIntent(TaskFormIntent.AddAttachment(it)) }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onIntent(TaskFormIntent.AddAttachment(it)) }
    }

    if (uiState.isLoading) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Carregando...") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
        return
    }

    // Custom reminder days dialog
    if (showCustomReminderDialog) {
        val customDays = customReminderText.toIntOrNull()
        val isCustomValid = customDays != null && customDays in 1..60
        AlertDialog(
            onDismissRequest = { showCustomReminderDialog = false },
            title = { Text("Personalizado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = customReminderText,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) customReminderText = it },
                        label = { Text("Número de dias") },
                        singleLine = true,
                        isError = customReminderText.isNotEmpty() && !isCustomValid,
                        supportingText = if (customReminderText.isNotEmpty() && !isCustomValid) {
                            { Text("Valor entre 1 e 60 dias") }
                        } else null
                    )
                    Text(
                        "Máximo: 60 dias antes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isCustomValid) {
                            viewModel.onIntent(TaskFormIntent.AddCustomAdvanceReminderDay(customDays!!))
                        }
                        customReminderText = ""
                        showCustomReminderDialog = false
                    },
                    enabled = isCustomValid
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    customReminderText = ""
                    showCustomReminderDialog = false
                }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.editingTaskId != null) "Editar Tarefa" else "Nova Tarefa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Title ──
            FormSectionLabel("Título")
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onIntent(TaskFormIntent.UpdateTitle(it)) },
                label = { Text("Título da tarefa") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.validationError != null && uiState.title.isBlank(),
                singleLine = true
            )

            // ── Date ──
            FormSectionLabel("Data")
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
            val isDateEnabled = uiState.isDateFieldEnabled
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    if (!isDateEnabled) {
                        PlainTooltip { Text("A data é definida pela configuração de periodicidade") }
                    }
                },
                state = rememberTooltipState()
            ) {
                OutlinedTextField(
                    value = uiState.date.format(dateFormatter),
                    onValueChange = {},
                    label = { Text("Data") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = isDateEnabled,
                    trailingIcon = if (isDateEnabled) {
                        { TextButton(onClick = { showDatePicker = true }) { Text("Alterar") } }
                    } else null,
                    supportingText = if (!isDateEnabled) {
                        { Text("Não aplicável para a periodicidade selecionada") }
                    } else null
                )
            }

            // ── Time ──
            FormSectionLabel("Horário")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("O dia todo", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.isAllDay,
                    onCheckedChange = { viewModel.onIntent(TaskFormIntent.ToggleAllDay(it)) }
                )
            }

            if (!uiState.isAllDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = formatTime(uiState.startHour, uiState.startMinute),
                        onValueChange = {},
                        label = { Text("Início") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            TextButton(onClick = { showStartTimePicker = true }) { Text("Alterar") }
                        }
                    )
                    OutlinedTextField(
                        value = formatTime(uiState.endHour, uiState.endMinute),
                        onValueChange = {},
                        label = { Text("Fim") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            TextButton(onClick = { showEndTimePicker = true }) { Text("Alterar") }
                        }
                    )
                }
            }

            // ── Recurrence ──
            FormSectionLabel("Periodicidade")
            val recurrenceOptions = RecurrenceTypeOption.entries
            val recurrenceLabels = listOf("Única", "Diária", "Semanal", "Mensal", "Anual")

            // Row 1: Única | Diária | Semanal
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                recurrenceOptions.take(3).forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = uiState.recurrenceType == option,
                        onClick = { viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(option)) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3)
                    ) { Text(recurrenceLabels[index], fontSize = 12.sp) }
                }
            }
            // Row 2: Mensal | Anual
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                recurrenceOptions.drop(3).forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = uiState.recurrenceType == option,
                        onClick = { viewModel.onIntent(TaskFormIntent.UpdateRecurrenceType(option)) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2)
                    ) { Text(recurrenceLabels[index + 3], fontSize = 12.sp) }
                }
            }

            // Days of week (Custom weekly)
            if (uiState.recurrenceType == RecurrenceTypeOption.CUSTOM_WEEKLY) {
                val dayLabels = mapOf(
                    DayOfWeek.MONDAY to "Seg", DayOfWeek.TUESDAY to "Ter",
                    DayOfWeek.WEDNESDAY to "Qua", DayOfWeek.THURSDAY to "Qui",
                    DayOfWeek.FRIDAY to "Sex", DayOfWeek.SATURDAY to "Sáb",
                    DayOfWeek.SUNDAY to "Dom"
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = uiState.selectedDays.contains(day),
                            onClick = { viewModel.onIntent(TaskFormIntent.ToggleDayOfWeek(day)) },
                            label = { Text(dayLabels[day] ?: day.name) }
                        )
                    }
                }
            }

            // Day of month picker (Monthly)
            if (uiState.recurrenceType == RecurrenceTypeOption.MONTHLY) {
                Text(
                    "Todo dia ${uiState.selectedDayOfMonth} do mês",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..31).forEach { day ->
                        FilterChip(
                            selected = uiState.selectedDayOfMonth == day,
                            onClick = { viewModel.onIntent(TaskFormIntent.UpdateDayOfMonth(day)) },
                            label = { Text("$day") }
                        )
                    }
                }
            }

            // Month + Day picker (Yearly)
            if (uiState.recurrenceType == RecurrenceTypeOption.YEARLY) {
                Text(
                    "Todo ano em ${MONTH_LABELS[uiState.selectedYearlyMonth - 1]} / dia ${uiState.selectedYearlyDay}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Month grid (4 cols)
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                    modifier = Modifier.height(132.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(12) { idx ->
                        val monthNum = idx + 1
                        val isSelected = uiState.selectedYearlyMonth == monthNum
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isSelected) 0.dp else 1.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.onIntent(TaskFormIntent.UpdateYearlyMonth(monthNum)) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = MONTH_LABELS[idx],
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                            else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Day of month
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Dia:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = uiState.selectedYearlyDay.toString(),
                        onValueChange = { v ->
                            val d = v.toIntOrNull()
                            if (d != null && d in 1..31) viewModel.onIntent(TaskFormIntent.UpdateYearlyDay(d))
                        },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }
            }

            // ── Advance Reminders ──
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("Lembretes gerados diariamente até o dia anterior à tarefa") } },
                state = rememberTooltipState()
            ) {
                FormSectionLabel("Lembrar antes")
            }
            Text(
                "Receba um lembrete diário a partir do número de dias selecionado até o dia anterior à tarefa. Ex: \"2 dias antes\" gera lembretes no 2º e no 1º dia antes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PRESET_REMINDER_DAYS.forEach { days ->
                    val isSelected = uiState.advanceReminderDays.contains(days)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(days)) },
                        label = { Text(if (days == 1) "1 dia antes" else "$days dias antes") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                // Custom reminder chip (non-preset, single value)
                uiState.advanceReminderDays.filter { it !in PRESET_REMINDER_DAYS }.forEach { days ->
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.onIntent(TaskFormIntent.ToggleAdvanceReminderDay(days)) },
                        label = { Text("$days dias antes") },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(14.dp))
                        }
                    )
                }
                // Add custom button (shown only when no custom value is active)
                if (uiState.advanceReminderDays.none { it !in PRESET_REMINDER_DAYS }) {
                    FilterChip(
                        selected = false,
                        onClick = { showCustomReminderDialog = true },
                        label = { Text("Personalizado") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar", modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }

            // ── Description ──
            FormSectionLabel("Descrição")
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onIntent(TaskFormIntent.UpdateDescription(it)) },
                label = { Text("Descrição (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // ── Images ──
            FormSectionLabel("Imagens")
            val imageAttachments = uiState.attachments.filter { isImageFile(it) }
            ImageAttachmentGrid(
                images = imageAttachments,
                onAddImage = { imagePicker.launch("image/*") },
                onRemoveImage = { viewModel.onIntent(TaskFormIntent.RemoveAttachment(it)) },
                onImageClick = { path -> selectedImagePath = path }
            )

            // ── Files ──
            FormSectionLabel("Arquivos")
            val fileAttachments = uiState.attachments.filter { !isImageFile(it) }
            FileAttachmentList(
                files = fileAttachments,
                onAddFile = { filePicker.launch("*/*") },
                onRemoveFile = { viewModel.onIntent(TaskFormIntent.RemoveAttachment(it)) },
                onOpenFile = { path -> openFile(path) }
            )

            // ── Image viewer overlay ──
            selectedImagePath?.let { path ->
                ImageViewerDialog(imagePath = path, onDismiss = { selectedImagePath = null })
            }

            // ── Checklist ──
            FormSectionLabel("Checklist")
            uiState.checklistItems.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { viewModel.onIntent(TaskFormIntent.ToggleChecklistItem(item.id)) }
                    )
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { viewModel.onIntent(TaskFormIntent.RemoveChecklistItem(item.id)) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remover")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newChecklistText,
                    onValueChange = { newChecklistText = it },
                    label = { Text("Novo item") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newChecklistText.isNotBlank()) {
                        viewModel.onIntent(TaskFormIntent.AddChecklistItem(newChecklistText))
                        newChecklistText = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar item")
                }
            }

            // ── Show in notification toggle ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Exibir na notificação", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Mostrar esta tarefa na tela de bloqueio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text("Quando ativo, a tarefa aparece na tela de bloqueio e nas notificações persistentes") } },
                    state = rememberTooltipState()
                ) {
                    Switch(
                        checked = uiState.showInNotification,
                        onCheckedChange = { viewModel.onIntent(TaskFormIntent.ToggleShowInNotification(it)) }
                    )
                }
            }

            // ── Validation error ──
            if (uiState.validationError != null) {
                Text(
                    text = uiState.validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── Save button ──
            Button(
                onClick = { viewModel.onIntent(TaskFormIntent.Save) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Salvar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onIntent(TaskFormIntent.UpdateDate(date))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            initialHour = uiState.startHour,
            initialMinute = uiState.startMinute,
            onConfirm = { hour, minute ->
                viewModel.onIntent(TaskFormIntent.UpdateStartTime(hour, minute))
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialHour = uiState.endHour,
            initialMinute = uiState.endMinute,
            onConfirm = { hour, minute ->
                viewModel.onIntent(TaskFormIntent.UpdateEndTime(hour, minute))
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
}

@Composable
private fun FormSectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.secondary)
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.1.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageAttachmentGrid(
    images: List<String>,
    onAddImage: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onImageClick: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        images.forEach { path ->
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(0.4f), RoundedCornerShape(10.dp))
                    .clickable { onImageClick(path) }
            ) {
                AsyncImage(
                    model = File(path),
                    contentDescription = "Imagem anexada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemoveImage(path) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remover",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        // Add button
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(0.5f), RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onAddImage() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Adicionar imagem",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Foto",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun FileAttachmentList(
    files: List<String>,
    onAddFile: () -> Unit,
    onRemoveFile: (String) -> Unit,
    onOpenFile: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        files.forEach { path ->
            val file = File(path)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onOpenFile(path) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = file.extension.uppercase().take(3).ifEmpty { "DOC" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = file.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sizeKb = file.length() / 1024
                Text(
                    text = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { onRemoveFile(path) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remover arquivo",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        // Add file button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    1.5.dp,
                    MaterialTheme.colorScheme.outline.copy(0.5f),
                    RoundedCornerShape(12.dp)
                )
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onAddFile() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                "Anexar arquivo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ImageViewerDialog(imagePath: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Visualizar imagem",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        TimePicker(state = state, modifier = Modifier.padding(16.dp))
    }
}

private fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

private fun isImageFile(path: String): Boolean {
    val lower = path.lowercase()
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".webp") ||
            lower.endsWith(".gif") || path.contains("/images/")
}
