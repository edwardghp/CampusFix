package com.campusfix.feature.technician

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusfix.domain.model.FaultCategory
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.TicketStatus
import com.campusfix.domain.model.Urgency
import java.text.SimpleDateFormat
import java.util.*

/**
 * HU07 - Cola de trabajo del tecnico con seguimiento en tiempo real.
 * - Listener de Firestore (vivo) con cache offline en Room.
 * - Filtros por estado y categoria.
 * - Ordenada por prioridad (urgencia).
 * - Permite avanzar el estado del ticket y navegar al aula con Google Maps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedTicketsScreen(
    onBack: () -> Unit,
    viewModel: AssignedTicketsViewModel = hiltViewModel()
) {
    val assignedTickets by viewModel.assignedTickets.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mi cola de trabajo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (showFilters) {
                FiltersBar(
                    filters = state.filters,
                    onEstadoSelected = viewModel::setEstadoFilter,
                    onCategoriaSelected = viewModel::setCategoriaFilter,
                )
            }

            if (assignedTickets.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.filters.estado != null || state.filters.categoria != null)
                            "No hay tickets que coincidan con el filtro."
                        else
                            "No tienes tareas asignadas en este momento.",
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(assignedTickets, key = { it.id }) { ticket ->
                        AssignedTicketItem(
                            ticket = ticket,
                            onAvanzarEstado = { viewModel.avanzarEstado(ticket) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltersBar(
    filters: AssignedTicketsFilters,
    onEstadoSelected: (TicketStatus?) -> Unit,
    onCategoriaSelected: (FaultCategory?) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Estado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filters.estado == null,
                    onClick = { onEstadoSelected(null) },
                    label = { Text("Todos") },
                )
            }
            // Solo estados relevantes para la cola del tecnico
            items(listOf(TicketStatus.ASIGNADO, TicketStatus.EN_ATENCION, TicketStatus.RESUELTO)) { estado ->
                FilterChip(
                    selected = filters.estado == estado,
                    onClick = { onEstadoSelected(if (filters.estado == estado) null else estado) },
                    label = { Text(estado.label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Categoria", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filters.categoria == null,
                    onClick = { onCategoriaSelected(null) },
                    label = { Text("Todas") },
                )
            }
            items(FaultCategory.entries.toList()) { cat ->
                FilterChip(
                    selected = filters.categoria == cat,
                    onClick = { onCategoriaSelected(if (filters.categoria == cat) null else cat) },
                    label = { Text(cat.label) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }
}

@Composable
fun AssignedTicketItem(ticket: Ticket, onAvanzarEstado: () -> Unit) {
    val context = LocalContext.current
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ticket.categoria.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (ticket.urgencia == Urgency.ALTA) {
                    Badge(containerColor = Color.Red, contentColor = Color.White) {
                        Text("URGENTE")
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(ticket.aulaNombre, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text(ticket.descripcion, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Estado: ${ticket.estado.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    fmt.format(Date(ticket.creadoEn)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // HU07 - Navegacion al aula con intent de Google Maps
                OutlinedButton(
                    onClick = { abrirEnGoogleMaps(context, ticket) },
                    enabled = ticket.aulaLat != null && ticket.aulaLng != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Como llegar")
                }
                // HU07 - Avanzar el estado del ticket (Asignado -> En atencion -> Resuelto)
                val siguiente = when (ticket.estado) {
                    TicketStatus.ASIGNADO -> "Iniciar atencion"
                    TicketStatus.EN_ATENCION -> "Marcar resuelto"
                    else -> null
                }
                if (siguiente != null) {
                    Button(onClick = onAvanzarEstado, modifier = Modifier.weight(1f)) {
                        Text(siguiente)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/** HU07 - Abre Google Maps (o cualquier app de navegacion) con la ubicacion del aula. */
private fun abrirEnGoogleMaps(context: android.content.Context, ticket: Ticket) {
    val lat = ticket.aulaLat ?: return
    val lng = ticket.aulaLng ?: return
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(ticket.aulaNombre)})")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Sin app de mapas instalada: caemos al navegador con Google Maps web
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}
