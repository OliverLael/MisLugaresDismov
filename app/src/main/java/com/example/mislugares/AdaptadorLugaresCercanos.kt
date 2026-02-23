package com.example.mislugares

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdaptadorLugaresCercanos(
    private var lugares: List<Lugar>,
    private val onItemClicked: (String) -> Unit
) : RecyclerView.Adapter<AdaptadorLugaresCercanos.ViewHolder>() {

    private lateinit var aplicacion: AplicacionMisLugares

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombreTextView: TextView = view.findViewById(R.id.nombre)
        val distanciaTextView: TextView = view.findViewById(R.id.distancia)
        val fotoImageView: ImageView = view.findViewById(R.id.foto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        aplicacion = parent.context.applicationContext as AplicacionMisLugares
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lugar_cercano, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lugar = lugares[position]
        holder.nombreTextView.text = lugar.nombre

        val posActual = aplicacion.posicionActual
        if (posActual != GeoPunto.SIN_POSICION && lugar.posicion != GeoPunto.SIN_POSICION) {
            val distancia = posActual.distancia(lugar.posicion)
            holder.distanciaTextView.text = when {
                distancia > 2000 -> "${"%.1f".format(distancia / 1000)} km"
                else -> "${distancia.toInt()} m"
            }
            holder.distanciaTextView.visibility = View.VISIBLE
        } else {
            holder.distanciaTextView.visibility = View.GONE
        }

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
            onItemClicked(lugar.id)
        }
    }

    override fun getItemCount() = lugares.size

    fun actualizarLugares(nuevosLugares: List<Lugar>) {
        this.lugares = nuevosLugares
        notifyDataSetChanged()
    }
}
