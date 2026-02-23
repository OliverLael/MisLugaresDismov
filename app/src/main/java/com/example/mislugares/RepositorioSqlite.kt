package com.example.mislugares

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.preference.PreferenceManager

class RepositorioSqlite(private val context: Context) : LugarRepositorio {

    private val bdHelper = LugaresBDHelper(context)
    private val bd: SQLiteDatabase = bdHelper.writableDatabase
    override var adaptador: AdaptadorLugares? = null

    private var lugaresCache: List<Lugar> = emptyList()

    override fun iniciarEscuchador(actualizar: (List<Lugar>) -> Unit) {
        lugaresCache = obtenerTodosSincrono()
        actualizar(lugaresCache)
    }

    override fun detenerEscuchador() { }

    override fun obtenerTodosSincrono(): List<Lugar> {
        val lugares = mutableListOf<Lugar>()
        val cursor = obtenerCursor()
        while (cursor?.moveToNext() == true) {
            lugares.add(extraeLugar(cursor))
        }
        cursor?.close()
        lugaresCache = lugares
        return lugares
    }

    private fun extraeLugar(cursor: Cursor): Lugar {
        val pendienteIdx = cursor.getColumnIndex(LugaresBDHelper.KEY_PENDIENTE_VISITA)
        val pendiente = if (pendienteIdx >= 0) cursor.getInt(pendienteIdx) == 1 else false
        return Lugar(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_ID)).toString(),
            nombre = cursor.getString(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_NOMBRE)),
            direccion = cursor.getString(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_DIRECCION)),
            posicion = GeoPunto(
                cursor.getDouble(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_LONGITUD)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_LATITUD))
            ),
            tipo = TipoLugar.values()[cursor.getInt(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_TIPO))],
            dificultad = Dificultad.values()[
                cursor.getInt(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_DIFICULTAD))
            ],
            fotoUri = cursor.getString(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_FOTO_URI)),
            telefono = cursor.getLong(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_TELEFONO)),
            url = cursor.getString(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_URL)),
            comentario = cursor.getString(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_COMENTARIO)),
            fecha = cursor.getLong(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_FECHA)),
            valoracion = cursor.getFloat(cursor.getColumnIndexOrThrow(LugaresBDHelper.KEY_VALORACION)),
            pendienteVisita = pendiente
        )
    }

    override fun obtenerCursor(): Cursor? {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val orden = pref.getString("orden", "1") ?: "1"
        val maximoStr = pref.getString("maximo", "12") ?: "12"
        val maximo = maximoStr.toIntOrNull() ?: 12

        var consulta = "SELECT * FROM ${LugaresBDHelper.TABLE_LUGARES}"
        var orderBy: String? = null

        when (orden) {
            "1" -> orderBy = "${LugaresBDHelper.KEY_NOMBRE} ASC"
            "2" -> orderBy = "${LugaresBDHelper.KEY_VALORACION} DESC"
            "3" -> {
                val appContext = context.applicationContext as? AplicacionMisLugares
                if (appContext != null && appContext.posicionActual != GeoPunto.SIN_POSICION) {
                    val lon = appContext.posicionActual.longitud
                    val lat = appContext.posicionActual.latitud
                    orderBy = "(${lon} - ${LugaresBDHelper.KEY_LONGITUD})*(${lon} - ${LugaresBDHelper.KEY_LONGITUD}) + " +
                            "(${lat} - ${LugaresBDHelper.KEY_LATITUD})*(${lat} - ${LugaresBDHelper.KEY_LATITUD})"
                } else {
                    orderBy = "${LugaresBDHelper.KEY_NOMBRE} ASC"
                }
            }
            else -> orderBy = "${LugaresBDHelper.KEY_NOMBRE} ASC"
        }

        consulta += " ORDER BY $orderBy LIMIT $maximo"
        return bd.rawQuery(consulta, null)
    }

    override fun elementoPorId(id: String): Lugar? {
        val longId = id.toLongOrNull() ?: return null
        val cursor = bd.query(
            LugaresBDHelper.TABLE_LUGARES, null,
            "${LugaresBDHelper.KEY_ID} = ?", arrayOf(longId.toString()),
            null, null, null
        )
        val lugar = if (cursor.moveToFirst()) extraeLugar(cursor) else null
        cursor.close()
        return lugar
    }

    override fun idPorPosicion(pos: Int): String {
        return if (pos >= 0 && pos < lugaresCache.size) {
            lugaresCache[pos].id
        } else {
            ""
        }
    }

    override fun añade(lugar: Lugar) {
        val valores = ContentValues().apply {
            put(LugaresBDHelper.KEY_NOMBRE, lugar.nombre)
            put(LugaresBDHelper.KEY_DIRECCION, lugar.direccion)
            put(LugaresBDHelper.KEY_LONGITUD, lugar.posicion.longitud)
            put(LugaresBDHelper.KEY_LATITUD, lugar.posicion.latitud)
            put(LugaresBDHelper.KEY_TIPO, lugar.tipo.ordinal)
            put(LugaresBDHelper.KEY_DIFICULTAD, lugar.dificultad.ordinal)
            put(LugaresBDHelper.KEY_FOTO_URI, lugar.fotoUri)
            put(LugaresBDHelper.KEY_TELEFONO, lugar.telefono)
            put(LugaresBDHelper.KEY_URL, lugar.url)
            put(LugaresBDHelper.KEY_COMENTARIO, lugar.comentario)
            put(LugaresBDHelper.KEY_FECHA, lugar.fecha)
            put(LugaresBDHelper.KEY_VALORACION, lugar.valoracion)
            put(LugaresBDHelper.KEY_PENDIENTE_VISITA, if (lugar.pendienteVisita) 1 else 0)
        }
        bd.insert(LugaresBDHelper.TABLE_LUGARES, null, valores)
        iniciarEscuchador { adaptador?.actualizarLugares(it) }
    }

    override fun tamaño(): Int {
        return lugaresCache.size
    }

    override fun borrarPorId(id: String): Boolean {
        val longId = id.toLongOrNull() ?: return false
        val count = bd.delete(
            LugaresBDHelper.TABLE_LUGARES,
            "${LugaresBDHelper.KEY_ID} = ?", arrayOf(longId.toString())
        )
        val success = count > 0
        if (success) {
            iniciarEscuchador { adaptador?.actualizarLugares(it) }
        }
        return success
    }

    override fun actualizaPorId(id: String, lugar: Lugar): Boolean {
        val longId = id.toLongOrNull() ?: return false
        val valores = ContentValues().apply {
            put(LugaresBDHelper.KEY_NOMBRE, lugar.nombre)
            put(LugaresBDHelper.KEY_DIRECCION, lugar.direccion)
            put(LugaresBDHelper.KEY_LONGITUD, lugar.posicion.longitud)
            put(LugaresBDHelper.KEY_LATITUD, lugar.posicion.latitud)
            put(LugaresBDHelper.KEY_TIPO, lugar.tipo.ordinal)
            put(LugaresBDHelper.KEY_DIFICULTAD, lugar.dificultad.ordinal)
            put(LugaresBDHelper.KEY_FOTO_URI, lugar.fotoUri)
            put(LugaresBDHelper.KEY_TELEFONO, lugar.telefono)
            put(LugaresBDHelper.KEY_URL, lugar.url)
            put(LugaresBDHelper.KEY_COMENTARIO, lugar.comentario)
            put(LugaresBDHelper.KEY_FECHA, lugar.fecha)
            put(LugaresBDHelper.KEY_VALORACION, lugar.valoracion)
            put(LugaresBDHelper.KEY_PENDIENTE_VISITA, if (lugar.pendienteVisita) 1 else 0)
        }
        val count = bd.update(
            LugaresBDHelper.TABLE_LUGARES, valores,
            "${LugaresBDHelper.KEY_ID} = ?", arrayOf(longId.toString())
        )
        val success = count > 0
        if (success) {
            iniciarEscuchador { adaptador?.actualizarLugares(it) }
        }
        return success
    }

    // ---- Nuevos filtros del dashboard ----

    override fun obtenerPorRangoKm(rangoKm: Float, posActual: GeoPunto): List<Lugar> {
        if (posActual == GeoPunto.SIN_POSICION) return emptyList()
        return obtenerTodos().filter { lugar ->
            lugar.posicion != GeoPunto.SIN_POSICION &&
                    posActual.distancia(lugar.posicion) <= rangoKm * 1000
        }
    }

    override fun obtenerPorDificultad(dificultad: Dificultad): List<Lugar> {
        return obtenerTodos().filter { it.dificultad == dificultad }
    }

    override fun obtenerPorTipo(tipo: TipoLugar): List<Lugar> {
        return obtenerTodos().filter { it.tipo == tipo }
    }

    override fun obtenerPendientes(): List<Lugar> {
        return obtenerTodos().filter { it.pendienteVisita }
    }

    override fun marcarPendiente(id: String, pendiente: Boolean) {
        val longId = id.toLongOrNull() ?: return
        val valores = ContentValues().apply {
            put(LugaresBDHelper.KEY_PENDIENTE_VISITA, if (pendiente) 1 else 0)
        }
        bd.update(
            LugaresBDHelper.TABLE_LUGARES, valores,
            "${LugaresBDHelper.KEY_ID} = ?", arrayOf(longId.toString())
        )
        iniciarEscuchador { adaptador?.actualizarLugares(it) }
    }

    private fun obtenerTodos(): List<Lugar> {
        val lugares = mutableListOf<Lugar>()
        val cursor = bd.rawQuery("SELECT * FROM ${LugaresBDHelper.TABLE_LUGARES} ORDER BY ${LugaresBDHelper.KEY_NOMBRE} ASC", null)
        while (cursor.moveToNext()) {
            lugares.add(extraeLugar(cursor))
        }
        cursor.close()
        return lugares
    }
}
