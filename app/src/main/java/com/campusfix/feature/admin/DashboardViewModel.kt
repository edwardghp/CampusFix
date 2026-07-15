package com.campusfix.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusfix.domain.model.DashboardMetrics
import com.campusfix.domain.usecase.GetDashboardMetricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * HU10 - ViewModel del dashboard de metricas y SLA del coordinador.
 * Expone un flujo de [DashboardMetrics] recalculado en tiempo real cada vez
 * que cambian los tickets en Firestore.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    getDashboardMetrics: GetDashboardMetricsUseCase,
) : ViewModel() {

    val metrics: StateFlow<DashboardMetrics> = getDashboardMetrics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardMetrics(),
        )
}
