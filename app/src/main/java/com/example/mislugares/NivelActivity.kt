package com.example.mislugares

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.databinding.ActivityNivelBinding

class NivelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNivelBinding
    private val repositorio: LugarRepositorio by lazy {
        (application as AplicacionMisLugares).repositorio
    }
    private val casosUsoLugar by lazy { CasosUsoLugar(this, repositorio) }
    private lateinit var adaptador: AdaptadorLugares
    private var dificultadActual: Dificultad = Dificultad.PRINCIPIANTE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNivelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarNivel)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarNivel.setNavigationOnClickListener { finish() }

        adaptador = AdaptadorLugares(emptyList()) { posicion ->
            val id = adaptador.idPorPosicion(posicion)
            if (id.isNotEmpty()) casosUsoLugar.mostrar(id)
        }

        binding.recyclerNivel.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@NivelActivity)
            adapter = adaptador
        }

        // Default to Principiante
        binding.chipGroupNivel.check(R.id.chip_principiante)
        cargarLugares(Dificultad.PRINCIPIANTE)

        binding.chipGroupNivel.setOnCheckedStateChangeListener { _, checkedIds ->
            val dif = when (checkedIds.firstOrNull()) {
                R.id.chip_intermedio -> Dificultad.INTERMEDIO
                R.id.chip_avanzado -> Dificultad.AVANZADO
                else -> Dificultad.PRINCIPIANTE
            }
            dificultadActual = dif
            cargarLugares(dif)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarLugares(dificultadActual)
    }

    private fun cargarLugares(dificultad: Dificultad) {
        val lugares = repositorio.obtenerPorDificultad(dificultad)
        adaptador.actualizarLugares(lugares)
        binding.emptyState.visibility = if (lugares.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerNivel.visibility = if (lugares.isEmpty()) View.GONE else View.VISIBLE
    }
}
