package com.campusfix.feature.technician

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.File

/**
 * HU08 - Pantalla de cierre del ticket (lado del tecnico).
 * Muestra el resumen de la falla reportada y un formulario para registrar la
 * solucion aplicada: descripcion, tiempo empleado y foto del equipo reparado.
 * Al enviar, el ticket pasa a estado "Resuelto".
 */

/** HU09 - Se añadio ongotochat y el boton para el asistente de IA. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    onCerrado: () -> Unit,
    onBack: () -> Unit,
    onGoToChat: (String) -> Unit,
    viewModel: TicketDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    // --- Captura de la foto del equipo reparado (misma logica que HU04) ---
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) pendingPhotoUri?.let { viewModel.onFoto(it) } }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            pendingPhotoUri = uri
            takePicture.launch(uri)
        }
    }

    LaunchedEffect(state.cerrado) { if (state.cerrado) onCerrado() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
        viewModel.consumeError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Cerrar ticket") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                },
                actions = {
                    IconButton(onClick = { state.ticket?.id?.let { onGoToChat(it) } }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Asistente IA")
                    }
                }
            )
        },
    ) { inner ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val ticket = state.ticket
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Resumen de la falla original reportada (HU04)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        ticket?.categoria?.label ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(ticket?.aulaNombre ?: "", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        ticket?.descripcion ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Text("Registrar solucion", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = state.solucionDescripcion,
                onValueChange = viewModel::onDescripcion,
                label = { Text("Descripcion de la solucion aplicada") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.tiempoEmpleado,
                onValueChange = viewModel::onTiempo,
                label = { Text("Tiempo empleado (minutos)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Foto del equipo reparado", style = MaterialTheme.typography.titleLarge)
            state.fotoSolucion?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Foto de la solucion",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            OutlinedButton(
                onClick = { cameraPermission.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (state.fotoSolucion == null) "Tomar foto" else "Tomar otra foto")
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::registrarSolucion,
                enabled = !state.guardando,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.guardando) CircularProgressIndicator(Modifier.height(20.dp))
                else Text("Registrar solucion y cerrar")
            }
            Text(
                "El ticket pasara a estado \"Resuelto\". El reportante podra calificar la " +
                        "atencion recibida y el ticket quedara \"Cerrado\" automaticamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** Crea un Uri temporal (via FileProvider) para que la camara guarde la foto. */
private fun createImageUri(context: android.content.Context): Uri {
    val file = File(context.cacheDir, "solucion_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
