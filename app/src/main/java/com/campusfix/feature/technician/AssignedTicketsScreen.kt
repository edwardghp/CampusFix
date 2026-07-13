package com.campusfix.feature.technician

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.Urgency
import java.text.SimpleDateFormat
import java.util.*
import com.campusfix.domain.model.TicketStatus
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedTicketsScreen(
    onBack: () -> Unit,
    // HU08 - al tocar un ticket, se navega a la pantalla de cierre/detalle
    onTicketClick: (String) -> Unit,
    viewModel: AssignedTicketsViewModel = hiltViewModel()
) {
    val assignedTickets by viewModel.assignedTickets.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Mis Tareas Asignadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                }
            )
        }
    ) { inner ->
        if (assignedTickets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("No tienes tareas asignadas en este momento.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(assignedTickets.sortedByDescending { it.urgencia == Urgency.ALTA }) { ticket ->
                    AssignedTicketItem(ticket = ticket,
                        // HU08 - un ticket ya CERRADO no se puede volver a abrir para cierre
                        onClick = { if (ticket.estado != TicketStatus.CERRADO) onTicketClick(ticket.id) },)
                }
            }
        }
    }
}

@Composable
fun AssignedTicketItem(ticket: Ticket,onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
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
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
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
                    color = MaterialTheme.colorScheme.outline
                )
            }
            // HU08 - pista visual de que el ticket ya tiene solucion registrada
            if (ticket.estado == TicketStatus.RESUELTO || ticket.estado == TicketStatus.CERRADO) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Solucion registrada" + if (ticket.tiempoEmpleadoMinutos != null)
                        " (${ticket.tiempoEmpleadoMinutos} min)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}
