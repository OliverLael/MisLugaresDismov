package com.example.mislugares

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdaptadorLugares(
    private var lugares: List<Lugar>,
    private val onItemClicked: (Int) -> Unit
) : RecyclerView.Adapter<AdaptadorLugares.ViewHolder>() {

    private lateinit var aplicacion: AplicacionMisLugares

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreTextView: TextView = view.findViewById(R.id.nombre)
        val direccionTextView: TextView = view.findViewById(R.id.direccion)
        val distanciaTextView: TextView = view.findViewById(R.id.distancia)
        val fotoImageView: ImageView = view.findViewById(R.id.foto)
        val valoracionRatingBar: RatingBar = view.findViewById(R.id.valoracion)
        val dificultadTextView: TextView = view.findViewById(R.id.dificultad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        aplicacion = parent.context.applicationContext as AplicacionMisLugares
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lugar, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lugar = lugares[position]
        holder.nombreTextView.text = lugar.nombre
        holder.direccionTextView.text = lugar.direccion.takeIf { !it.isNullOrEmpty() } ?: "Sin dirección"
        holder.valoracionRatingBar.rating = lugar.valoracion

        val posActual = aplicacion.posicionActual
        if (posActual != GeoPunto.SIN_POSICION && lugar.posicion != GeoPunto.SIN_POSICION) {
            val distancia = posActual.distancia(lugar.posicion)
            val textoDistancia = when {
                distancia > 2000 -> "${"%.1f".format(distancia / 1000)} km"
                distancia >= 0 -> "${distancia.toInt()} m"
                else -> "..."
            }
            holder.distanciaTextView.text = textoDistancia
            holder.distanciaTextView.visibility = View.VISIBLE
        } else {
            holder.distanciaTextView.visibility = View.GONE
        }

        // Difficulty badge with color
        val (textoNivel, colorHex) = when (lugar.dificultad) {
            Dificultad.PRINCIPIANTE -> Pair("Principiante", "#43A047")
            Dificultad.INTERMEDIO -> Pair("Intermedio", "#FB8C00")
            Dificultad.AVANZADO -> Pair("Avanzado", "#E53935")
        }
        holder.dificultadTextView.text = textoNivel
        holder.dificultadTextView.setTextColor(Color.WHITE)
        holder.dificultadTextView.background?.setTint(Color.parseColor(colorHex))

        if (!lugar.fotoUri.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(Uri.parse(lugar.fotoUri))
                .placeholder(R.drawable.mapa)
                .error(R.drawable.mapa)
                .into(holder.fotoImageView)
        } else {
            Glide.with(holder.itemView.context)
                .load(R.drawable.mapa)
                .into(holder.fotoImageView)
        }

        holder.itemView.setOnClickListener {
            onItemClicked(position)
        }
    }

    override fun getItemCount() = lugares.size

    fun actualizarLugares(nuevosLugares: List<Lugar>) {
        this.lugares = nuevosLugares
        notifyDataSetChanged()
    }

    fun idPorPosicion(pos: Int): String {
        return if (pos >= 0 && pos < lugares.size) lugares[pos].id else ""
    }
}
