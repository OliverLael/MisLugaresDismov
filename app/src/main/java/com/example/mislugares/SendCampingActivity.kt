package com.example.mislugares

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.databinding.ActivitySendCampingBinding

class SendCampingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendCampingBinding
    private val repositorio: LugarRepositorio by lazy {
        (application as AplicacionMisLugares).repositorio
    }
    private val casosUsoLugar by lazy { CasosUsoLugar(this, repositorio) }
    private lateinit var adaptador: AdaptadorLugares

    // null = todos, TipoLugar = filtrado
    private var tipoFiltro: TipoLugar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendCampingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSendcamp)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarSendcamp.setNavigationOnClickListener { finish() }

        adaptador = AdaptadorLugares(emptyList()) { posicion ->
            val id = adaptador.idPorPosicion(posicion)
            if (id.isNotEmpty()) casosUsoLugar.mostrar(id)
        }

        binding.recyclerSendcamp.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@SendCampingActivity)
            adapter = adaptador
        }

        binding.fabSendcamp.setOnClickListener {
            startActivity(Intent(this, EdicionLugarActivity::class.java))
        }

        // Default to Todos
        binding.chipGroupTipo.check(R.id.chip_todos)
        cargarLugares(null)

        binding.chipGroupTipo.setOnCheckedStateChangeListener { _, checkedIds ->
            tipoFiltro = when (checkedIds.firstOrNull()) {
                R.id.chip_senderismo -> TipoLugar.SENDERISMO
                R.id.chip_camping -> TipoLugar.CAMPING
                else -> null
            }
            cargarLugares(tipoFiltro)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarLugares(tipoFiltro)
    }

    private fun cargarLugares(tipo: TipoLugar?) {
        val lugares = when (tipo) {
            TipoLugar.SENDERISMO -> repositorio.obtenerPorTipo(TipoLugar.SENDERISMO)
            TipoLugar.CAMPING -> repositorio.obtenerPorTipo(TipoLugar.CAMPING)
            else -> {
                // Todos: SENDERISMO + CAMPING
                val s = repositorio.obtenerPorTipo(TipoLugar.SENDERISMO)
                val c = repositorio.obtenerPorTipo(TipoLugar.CAMPING)
                (s + c).sortedBy { it.nombre }
            }
        }
        adaptador.actualizarLugares(lugares)
        binding.emptyState.visibility = if (lugares.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerSendcamp.visibility = if (lugares.isEmpty()) View.GONE else View.VISIBLE
    }
}
