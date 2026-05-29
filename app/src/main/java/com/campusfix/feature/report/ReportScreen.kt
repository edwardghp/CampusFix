package com.campusfix.feature.report

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.campusfix.data.util.AudioRecorder
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.model.Urgency
import java.io.File
import androidx.compose.ui.tooling.preview.Preview
/** HU04 - Formulario de reporte: categoria, urgencia, descripcion, fotos y audio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onTicketSent: () -> Unit,
    onBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    // --- Captura de foto con la camara ---
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) pendingPhotoUri?.let { viewModel.addFoto(it) } }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            pendingPhotoUri = uri
            takePicture.launch(uri)
        }
    }

    // --- Grabacion de audio ---
    val recorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    val audioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recorder.start()
            isRecording = true
        }
    }

    LaunchedEffect(state.sent) { if (state.sent) onTicketSent() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
        viewModel.consumeError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Reportar falla") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Datos del aula (pre-rellenados desde el QR escaneado - HU03)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Aula", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline)
                    Text(
                        state.aula?.let { "${it.codigo} - ${it.nombre}" } ?: "Cargando...",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    state.aula?.let {
                        Text("${it.edificio}  -  ${it.facultad}",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text("Tipo de falla", style = MaterialTheme.typography.titleLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FaultCategory.entries) { cat ->
                    FilterChip(
                        selected = state.categoria == cat,
                        onClick = { viewModel.onCategoria(cat) },
                        label = { Text(cat.label) },
                    )
                }
            }

            Text("Urgencia", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Urgency.entries.forEach { u ->
                    FilterChip(
                        selected = state.urgencia == u,
                        onClick = { viewModel.onUrgencia(u) },
                        label = { Text(u.label) },
                    )
                }
            }

            OutlinedTextField(
                value = state.descripcion,
                onValueChange = viewModel::onDescripcion,
                label = { Text("Descripcion de la falla") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            // Fotos (HU04)
            Text("Fotos (maximo 3)", style = MaterialTheme.typography.titleLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.fotos) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Foto de la falla",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
            }
            OutlinedButton(
                onClick = { cameraPermission.launch(Manifest.permission.CAMERA) },
                enabled = state.fotos.size < 3,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Tomar foto") }

            // Audio (HU04)
            Text("Nota de voz (opcional)", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(
                onClick = {
                    if (isRecording) {
                        val file = recorder.stop()
                        isRecording = false
                        if (file != null) viewModel.setAudio(Uri.fromFile(file))
                    } else {
                        audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    when {
                        isRecording -> "Detener grabacion"
                        state.audio != null -> "Audio grabado - volver a grabar"
                        else -> "Grabar nota de voz"
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::send,
                enabled = !state.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.sending) CircularProgressIndicator(Modifier.height(20.dp))
                else Text("Enviar reporte")
            }
            Text(
                "El reporte se guarda en tu dispositivo y se envia automaticamente " +
                    "cuando haya conexion.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** Crea un Uri temporal (via FileProvider) para que la camara guarde la foto. */
private fun createImageUri(context: android.content.Context): Uri {
    val file = File(context.cacheDir, "foto_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
