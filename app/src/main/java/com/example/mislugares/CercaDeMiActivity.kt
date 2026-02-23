package com.example.mislugares

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.databinding.ActivityCercaDeMiBinding

class CercaDeMiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCercaDeMiBinding
    private val repositorio: LugarRepositorio by lazy {
        (application as AplicacionMisLugares).repositorio
    }
    private val casosUsoLugar by lazy { CasosUsoLugar(this, repositorio) }
    private lateinit var adaptador: AdaptadorLugares
    private var rangoKm: Float = 10f

    companion object {
        const val EXTRA_RANGO_KM = "extra_rango_km"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCercaDeMiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rangoKm = intent.getFloatExtra(EXTRA_RANGO_KM, 10f)

        setSupportActionBar(binding.toolbarCerca)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarCerca.setNavigationOnClickListener { finish() }

        adaptador = AdaptadorLugares(emptyList()) { posicion ->
            val id = adaptador.idPorPosicion(posicion)
            if (id.isNotEmpty()) casosUsoLugar.mostrar(id)
        }

        binding.recyclerCerca.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@CercaDeMiActivity)
            adapter = adaptador
        }

        binding.fabCerca.setOnClickListener {
            startActivity(Intent(this, EdicionLugarActivity::class.java))
        }

        cargarLugares()
    }

    override fun onResume() {
        super.onResume()
        cargarLugares()
    }

    private fun cargarLugares() {
        val app = application as AplicacionMisLugares
        val posicion = app.posicionActual
        val lugares = repositorio.obtenerPorRangoKm(rangoKm, posicion)

        adaptador.actualizarLugares(lugares)

        val titulo = "Cerca de Mí (${rangoKm.toInt()} km)"
        val subtitulo = "${lugares.size} lugar${if (lugares.size != 1) "es" else ""} encontrado${if (lugares.size != 1) "s" else ""}"
        binding.toolbarCerca.title = titulo
        binding.toolbarCerca.subtitle = subtitulo

        binding.emptyState.visibility = if (lugares.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerCerca.visibility = if (lugares.isEmpty()) View.GONE else View.VISIBLE
    }
}
