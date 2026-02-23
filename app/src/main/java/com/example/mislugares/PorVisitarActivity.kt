package com.example.mislugares

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.databinding.ActivityPorVisitarBinding

class PorVisitarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPorVisitarBinding
    private val repositorio: LugarRepositorio by lazy {
        (application as AplicacionMisLugares).repositorio
    }
    private val casosUsoLugar by lazy { CasosUsoLugar(this, repositorio) }
    private lateinit var adaptador: AdaptadorLugares

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPorVisitarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarVisitar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarVisitar.setNavigationOnClickListener { finish() }

        adaptador = AdaptadorLugares(emptyList()) { posicion ->
            val id = adaptador.idPorPosicion(posicion)
            if (id.isNotEmpty()) casosUsoLugar.mostrar(id)
        }

        binding.recyclerVisitar.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@PorVisitarActivity)
            adapter = adaptador
        }

        cargarLugares()
    }

    override fun onResume() {
        super.onResume()
        cargarLugares()
    }

    private fun cargarLugares() {
        val lugares = repositorio.obtenerPendientes()
        adaptador.actualizarLugares(lugares)
        binding.emptyState.visibility = if (lugares.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerVisitar.visibility = if (lugares.isEmpty()) View.GONE else View.VISIBLE
    }
}
