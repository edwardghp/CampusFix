package com.campusfix.core

/**
 * Envoltura del resultado de una operacion (exito / error / cargando).
 * Se usa en ViewModels y repositorios para representar estados de UI.
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}
