package com.example.alcoolougasolina

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.Manifest
import android.content.res.Resources
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mMap: GoogleMap
    private var savedMarkerLocation: LatLng? = null
    private lateinit var placesClient: PlacesClient
    private val orangeMarkers = mutableListOf<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.api_key))
        }

        placesClient = Places.createClient(this)

        initUI()
    }

    private fun initUI() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val btnSaveHistory = findViewById<Button>(R.id.btnSaveHistory)
        val btnViewHistory = findViewById<Button>(R.id.btnViewHistory)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnSaveHistory.setOnClickListener {
            if (savedMarkerLocation != null) {
                val intent = Intent(this, InputPricesActivity::class.java)
                intent.putExtra("latitude", savedMarkerLocation?.latitude)
                intent.putExtra("longitude", savedMarkerLocation?.longitude)
                startActivity(intent)
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Por favor, selecione um local no mapa primeiro!")
                builder.show()
            }
        }

        btnViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun checkLocationPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setMapStyle()

        if (checkLocationPermissions()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Local atual")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                } else {
                    Toast.makeText(
                        this,
                        "Não foi possível obter a localização atual",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        loadSavedMarkers()
        mMap.setOnMapClickListener { latLng ->
            clearOrangeMarkers()
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Novo Local")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
            savedMarkerLocation = latLng
            if (marker != null) {
                orangeMarkers.add(marker)
            }
        }
    }

    private fun loadSavedMarkers() {
        val history = sharedPreferences.getString("history", "")
        if (!history.isNullOrEmpty()) {
            val lines = history.split("\n\n")
            for (line in lines) {
                val regex = "📍 Localização: (.+?)/".toRegex()
                val matchResult = regex.find(line)
                if (matchResult != null) {
                    val address = matchResult.groupValues[1]
                    val geocoder = Geocoder(this)
                    val locationList = geocoder.getFromLocationName(address, 1)
                    if (locationList != null) {
                        if (locationList.isNotEmpty()) {
                            val location = locationList[0]
                            val latLng = LatLng(location.latitude, location.longitude)
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title("Histórico")
                                    .icon(
                                        BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_BLUE
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setMapStyle() {
        try {
            val success = mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
            if (!success) {
                Log.e("MapStyle", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MapStyle", "Can't find style. Error: ", e)
        }
    }

    private fun clearOrangeMarkers() {
        for (marker in orangeMarkers) {
            marker.remove()
        }
        orangeMarkers.clear()
    }
}
