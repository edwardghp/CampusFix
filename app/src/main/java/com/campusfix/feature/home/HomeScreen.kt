package com.campusfix.feature.home


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.campusfix.domain.model.Ticket
import com.campusfix.domain.model.TicketStatus
import com.campusfix.domain.model.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pantalla principal (Sprint 1). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onReportFault: () -> Unit,
    onEditProfile: () -> Unit,
    onLoggedOut: () -> Unit,
    onGoToAssignment: () -> Unit,
    onGoToTechManagement: () -> Unit,
    onGoToAssignedTickets: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CampusFix") },
                actions = {
                    if (state.user.rol == UserRole.COORDINADOR) {
                        IconButton(onClick = onGoToTechManagement) {
                            Icon(Icons.Default.People, contentDescription = "Gestionar técnicos")
                        }
                        IconButton(onClick = onGoToAssignment) {
                            Icon(Icons.Default.Assignment, contentDescription = "Asignar tickets")
                        }
                    }
                    if (state.user.rol == UserRole.TECNICO) {
                        IconButton(onClick = onGoToAssignedTickets) {
                            Icon(Icons.Default.Assignment, contentDescription = "Mis tareas")
                        }
                    }
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                    IconButton(onClick = { viewModel.logout(); onLoggedOut() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesion")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onReportFault,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Reportar falla") },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Hola, ${state.user.nombre.ifBlank { "usuario" }}",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                state.user.rol.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(16.dp))

            Text("Mis reportes", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            if (state.tickets.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Aun no tienes reportes.\nPulsa \"Reportar falla\" para crear uno.",
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.tickets) { ticket -> TicketCard(ticket) }
                }
            }
        }
    }
}


@Composable
private fun TicketCard(ticket: Ticket) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ticket.categoria.label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                EstadoBadge(estado = ticket.estado)
            }
            Text(ticket.aulaNombre, style = MaterialTheme.typography.bodyMedium)
            Text(
                ticket.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(4.dp))
            val sync = if (ticket.sincronizado) "Sincronizado" else "Pendiente de envio"
            Text(
                "$sync  -  Actualizado: ${fmt.format(Date(ticket.actualizadoEn))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** HU07 - Indicador visual del estado del ticket para el reportante. */
@Composable
private fun EstadoBadge(estado: TicketStatus) {
    val color = when (estado) {
        TicketStatus.ABIERTO -> MaterialTheme.colorScheme.outline
        TicketStatus.ASIGNADO -> MaterialTheme.colorScheme.tertiary
        TicketStatus.EN_ATENCION -> MaterialTheme.colorScheme.primary
        TicketStatus.RESUELTO -> Color(0xFF2E7D32)
        TicketStatus.CERRADO -> MaterialTheme.colorScheme.outline
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(estado.label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color.copy(alpha = 0.15f),
            disabledLabelColor = color,
        ),
    )
}