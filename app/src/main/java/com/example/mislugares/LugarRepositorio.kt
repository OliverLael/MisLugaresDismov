package com.example.mislugares

import android.database.Cursor

interface LugarRepositorio {
    var adaptador: AdaptadorLugares?

    fun iniciarEscuchador(actualizar: (List<Lugar>) -> Unit)
    fun detenerEscuchador()
    fun obtenerTodosSincrono(): List<Lugar>
    fun elementoPorId(id: String): Lugar?
    fun idPorPosicion(pos: Int): String
    fun añade(lugar: Lugar)
    fun borrarPorId(id: String): Boolean
    fun actualizaPorId(id: String, lugar: Lugar): Boolean
    fun tamaño(): Int
    fun obtenerCursor(): Cursor?

    // Nuevos filtros para las pantallas del dashboard
    fun obtenerPorRangoKm(rangoKm: Float, posActual: GeoPunto): List<Lugar>
    fun obtenerPorDificultad(dificultad: Dificultad): List<Lugar>
    fun obtenerPorTipo(tipo: TipoLugar): List<Lugar>
    fun obtenerPendientes(): List<Lugar>
    fun marcarPendiente(id: String, pendiente: Boolean)
}
