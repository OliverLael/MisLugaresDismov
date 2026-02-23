package com.example.mislugares

import android.content.Intent
import android.content.SharedPreferences
import android.location.Geocoder
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.mislugares.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repositorio: LugarRepositorio by lazy {
        (application as AplicacionMisLugares).repositorio
    }

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences

    // Range selection in km (default 10)
    private var rangoKmSeleccionado: Float = 10f

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                casosUsoLocalizacion.permisoConcedido()
                cargarClima()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        configurarMusica()
        configurarRangeSelector()
        configurarCards()
        configurarBotonesHeader()

        casosUsoLocalizacion.ultimaLocalizacion()
        cargarClima()
    }

    private fun configurarMusica() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.musica_fondo)
            mediaPlayer?.isLooping = true
        }
    }

    private fun configurarRangeSelector() {
        // Restore saved range
        rangoKmSeleccionado = prefs.getFloat("radio_busqueda", 10f)
        val btnId = when (rangoKmSeleccionado) {
            5f -> R.id.btn_5km
            25f -> R.id.btn_25km
            50f -> R.id.btn_50km
            else -> R.id.btn_10km
        }
        binding.toggleRange.check(btnId)

        binding.toggleRange.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                rangoKmSeleccionado = when (checkedId) {
                    R.id.btn_5km -> 5f
                    R.id.btn_25km -> 25f
                    R.id.btn_50km -> 50f
                    else -> 10f
                }
                prefs.edit().putFloat("radio_busqueda", rangoKmSeleccionado).apply()
            }
        }
    }

    private fun configurarCards() {
        binding.cardCerca.setOnClickListener {
            val intent = Intent(this, CercaDeMiActivity::class.java)
            intent.putExtra(CercaDeMiActivity.EXTRA_RANGO_KM, rangoKmSeleccionado)
            startActivity(intent)
        }

        binding.cardVisitar.setOnClickListener {
            startActivity(Intent(this, PorVisitarActivity::class.java))
        }

        binding.cardNivel.setOnClickListener {
            startActivity(Intent(this, NivelActivity::class.java))
        }

        binding.cardSenderismo.setOnClickListener {
            startActivity(Intent(this, SendCampingActivity::class.java))
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(this, EdicionLugarActivity::class.java))
        }
    }

    private fun configurarBotonesHeader() {
        binding.btnMapa.setOnClickListener {
            startActivity(Intent(this, MapaActivity::class.java))
        }
        binding.btnPreferencias.setOnClickListener {
            preferenciasLauncher.launch(Intent(this, PreferenciasActivity::class.java))
        }
    }

    private fun cargarClima() {
        val app = application as AplicacionMisLugares
        val posicion = app.posicionActual

        if (posicion == GeoPunto.SIN_POSICION) {
            binding.tvCondicion.text = getString(R.string.weather_unavailable)
            return
        }

        lifecycleScope.launch {
            val clima = WeatherService.obtenerClima(posicion.latitud, posicion.longitud)
            if (clima != null) {
                binding.tvTemp.text = "${clima.tempCelsius}°C"
                binding.tvEmoji.text = clima.emoji
                binding.tvCondicion.text = clima.condicion
                // Try to get city name from coordinates
                try {
                    val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(posicion.latitud, posicion.longitud, 1)
                    val ciudad = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: ""
                    binding.tvCiudad.text = ciudad
                } catch (_: Exception) {
                    binding.tvCiudad.text = ""
                }
            } else {
                binding.tvCondicion.text = getString(R.string.weather_unavailable)
            }
        }
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
            if (it.isPlaying) it.pause()
            if (liberarRecursos) {
                it.release()
                mediaPlayer = null
            }
        }
    }

    override fun onResume() {
        super.onResume()
        casosUsoLocalizacion.activarProveedores()
        iniciarMusica()
        cargarClima()
    }

    override fun onPause() {
        super.onPause()
        casosUsoLocalizacion.desactivarProveedores()
        pararMusica(liberarRecursos = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        pararMusica(liberarRecursos = true)
    }
}
