package com.example.mislugares

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mislugares.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repositorio: LugarRepositorio by lazy {
        (application as AplicacionMisLugares).repositorio
    }
    private val casosUsoLugar by lazy { CasosUsoLugar(this, repositorio) }

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences

    private lateinit var adaptador: AdaptadorLugares
    private lateinit var adaptadorCercanos: AdaptadorLugaresCercanos
    private var listaCompleta: List<Lugar> = emptyList()
    private var filtroNivel: Dificultad? = null

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                casosUsoLocalizacion.permisoConcedido()
            }
        }
    private val casosUsoLocalizacion: CasosUsoLocalizacion by lazy {
        CasosUsoLocalizacion(this, requestPermissionLauncher)
    }
    private val preferenciasLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        finish()
        startActivity(intent)
    }

    companion object {
        var debeRefrescarLista = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.musica_fondo)
            mediaPlayer?.isLooping = true
        }

        // Adapter principal
        adaptador = AdaptadorLugares(emptyList()) { idLugar ->
            if (idLugar.isNotEmpty()) {
                casosUsoLugar.mostrar(idLugar)
            }
        }
        repositorio.adaptador = adaptador

        binding.contentMain.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = adaptador
        }

        // Adapter senderismo y camping (horizontal)
        adaptadorCercanos = AdaptadorLugaresCercanos(emptyList()) { idLugar ->
            casosUsoLugar.mostrar(idLugar)
        }
        (application as AplicacionMisLugares).adaptadorCercanos = adaptadorCercanos

        binding.contentMain.recyclerCercanos.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = adaptadorCercanos
        }

        // Filtro de chips por nivel de dificultad
        binding.contentMain.chipGroupNivel.setOnCheckedStateChangeListener { _, checkedIds ->
            filtroNivel = when {
                checkedIds.contains(R.id.chipPrincipiante) -> Dificultad.PRINCIPIANTE
                checkedIds.contains(R.id.chipIntermedio) -> Dificultad.INTERMEDIO
                checkedIds.contains(R.id.chipAvanzado) -> Dificultad.AVANZADO
                else -> null
            }
            aplicarFiltro()
        }

        binding.fab.setOnClickListener {
            val intent = Intent(this, EdicionLugarActivity::class.java)
            startActivity(intent)
        }

        casosUsoLocalizacion.ultimaLocalizacion()
    }

    private fun aplicarFiltro() {
        val listaFiltrada = if (filtroNivel == null) {
            listaCompleta
        } else {
            listaCompleta.filter { it.dificultad == filtroNivel }
        }
        adaptador.actualizarLugares(listaFiltrada)
    }

    private fun actualizarCercanos() {
        val posActual = (application as AplicacionMisLugares).posicionActual
        val lugaresSenderismo = listaCompleta.filter { it.tipo == TipoLugar.NATURALEZA }
        val lugaresOrdenados = if (posActual != GeoPunto.SIN_POSICION) {
            lugaresSenderismo.sortedBy { lugar ->
                if (lugar.posicion != GeoPunto.SIN_POSICION) posActual.distancia(lugar.posicion)
                else Double.MAX_VALUE
            }
        } else {
            lugaresSenderismo.sortedBy { it.nombre }
        }
        adaptadorCercanos.actualizarLugares(lugaresOrdenados)
        binding.contentMain.seccionSenderismo.visibility =
            if (lugaresOrdenados.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun iniciarMusica() {
        mediaPlayer?.let {
            if (prefs.getBoolean("musica_habilitada", true) && !it.isPlaying) {
                try {
                    it.start()
                } catch (e: IllegalStateException) {
                    android.util.Log.e("MainActivity", "Error al iniciar MediaPlayer", e)
                }
            }
        }
    }

    private fun pararMusica(liberarRecursos: Boolean = false) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
            if (liberarRecursos) {
                it.release()
                mediaPlayer = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        repositorio.iniciarEscuchador { listaActualizada ->
            listaCompleta = listaActualizada
            aplicarFiltro()
            actualizarCercanos()
        }
        casosUsoLocalizacion.activarProveedores()
        iniciarMusica()
    }

    override fun onPause() {
        super.onPause()
        casosUsoLocalizacion.desactivarProveedores()
        pararMusica(liberarRecursos = false)
        repositorio.detenerEscuchador()
    }

    override fun onDestroy() {
        super.onDestroy()
        pararMusica(liberarRecursos = true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_mapa -> {
                val intent = Intent(this, MapaActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_preferencias -> {
                val intent = Intent(this, PreferenciasActivity::class.java)
                preferenciasLauncher.launch(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
