package com.campusfix.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.Urgency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentScreen(
    onBack: () -> Unit,
    viewModel: AssignmentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val openTickets by viewModel.openTickets.collectAsStateWithLifecycle()
    val technicians by viewModel.techniciansWithLoad.collectAsStateWithLifecycle()
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
                title = { Text("Asignacion de Tickets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
        ) {
            Text(
                text = "Tickets Abiertos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (openTickets.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay tickets pendientes de asignacion.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(openTickets.sortedByDescending { it.urgencia == Urgency.ALTA }) { ticket ->
                        TicketAssignmentItem(
                            ticket = ticket,
                            isSelected = state.selectedTicket?.id == ticket.id,
                            onClick = { viewModel.selectTicket(ticket) }
                        )
                    }
                }

                if (state.selectedTicket != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Asignar a Tecnico",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(technicians) { tech ->
                            TechnicianItem(
                                tech = tech,
                                onAssign = { viewModel.assign(tech.user) },
                                isAssigning = state.assigning
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketAssignmentItem(
    ticket: Ticket,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ticket.aulaNombre, fontWeight = FontWeight.Bold)
                Text("${ticket.categoria.label} • ${ticket.urgencia.label}", style = MaterialTheme.typography.bodySmall)
                Text(ticket.descripcion, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
            }
            if (ticket.urgencia == Urgency.ALTA) {
                Badge(containerColor = Color.Red, contentColor = Color.White) {
                    Text("URGENTE")
                }
            }
        }
    }
}

@Composable
fun TechnicianItem(
    tech: TechnicianWithLoad,
    onAssign: () -> Unit,
    isAssigning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tech.user.nombre, fontWeight = FontWeight.Bold)
                    if (tech.isRecommended) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Star, contentDescription = "Recomendado", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    }
                }
                Text("Especialidad: ${tech.user.especialidad?.label ?: "General"}", style = MaterialTheme.typography.bodySmall)
                Text("Carga actual: ${tech.currentLoad} tickets", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = onAssign,
                enabled = !isAssigning,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Asignar")
            }
        }
    }
}
