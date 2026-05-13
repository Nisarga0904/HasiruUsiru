package com.hasiru.usiru

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.clustering.ClusterManager
import com.hasiru.usiru.data.HasiruDatabase
import com.hasiru.usiru.data.Species
import com.hasiru.usiru.data.TreeHealth
import com.hasiru.usiru.data.TreeTag
import com.hasiru.usiru.data.TreeType
import com.hasiru.usiru.sync.FirebaseTreeSync
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val db by lazy { HasiruDatabase.get(this) }
    private val dao by lazy { db.treeTagDao() }
    private val sync by lazy { FirebaseTreeSync(dao) }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val fusedLocation by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private lateinit var root: LinearLayout
    private lateinit var content: FrameLayout
    private lateinit var bottomNav: BottomNavigationView
    private var googleMap: GoogleMap? = null
    private var clusterManager: ClusterManager<TreeClusterItem>? = null
    private var selectedLatLng: LatLng? = null
    private var lastLocation: Location? = null
    private var latestTags: List<TreeTag> = emptyList()
    private var capturedPhoto: Bitmap? = null

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { enableLocationIfAllowed() }

    private val photoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        capturedPhoto = bitmap
        Toast.makeText(this, "Photo added. Species will be simulated.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun showLogin() {
        val login = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(color(R.color.mist))
        }
        login.addView(TextView(this).apply {
            text = "Hasiru Usiru"
            textSize = 34f
            setTextColor(color(R.color.leaf_dark))
            gravity = Gravity.CENTER
        })
        login.addView(TextView(this).apply {
            text = "Community Green Auditor for Bengaluru and Mysuru"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
        })
        val email = TextInputEditText(this).apply {
            hint = "Email"
            setText("student@hasiru.local")
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val password = TextInputEditText(this).apply {
            hint = "Password"
            setText("password123")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        login.addView(email, matchWrap())
        login.addView(password, matchWrap())
        login.addView(Button(this).apply {
            text = "Login"
            setOnClickListener {
                if (email.text.isNullOrBlank() || password.text.isNullOrBlank()) {
                    toast("Enter email and password")
                } else {
                    auth.signInAnonymously()
                    showApp()
                }
            }
        }, matchWrap())
        login.addView(Button(this).apply {
            text = "Continue offline"
            setOnClickListener { showApp() }
        }, matchWrap())
        setContentView(login)
    }

    private fun showApp() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.mist))
        }
        content = FrameLayout(this).apply { id = View.generateViewId() }
        bottomNav = BottomNavigationView(this).apply {
            menu.add(0, NAV_MAP, 0, "Map").setIcon(android.R.drawable.ic_dialog_map)
            menu.add(0, NAV_TAG, 1, "Tag").setIcon(android.R.drawable.ic_menu_add)
            menu.add(0, NAV_SCORE, 2, "Score").setIcon(android.R.drawable.ic_menu_compass)
            menu.add(0, NAV_GUIDE, 3, "Guide").setIcon(android.R.drawable.ic_menu_info_details)
            setOnItemSelectedListener {
                when (it.itemId) {
                    NAV_MAP -> showMap()
                    NAV_TAG -> showTagger()
                    NAV_SCORE -> showScore()
                    NAV_GUIDE -> showGuide()
                }
                true
            }
        }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bottomNav, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        observeTags()
        bottomNav.selectedItemId = NAV_MAP
    }

    private fun observeTags() {
        lifecycleScope.launch {
            dao.observeAll().collectLatest { tags ->
                latestTags = tags
                refreshClusters(tags)
                if (::bottomNav.isInitialized && bottomNav.selectedItemId == NAV_SCORE) showScore()
            }
        }
    }

    private fun showMap() {
        content.removeAllViews()
        val mapId = View.generateViewId()
        content.addView(FrameLayout(this).apply { id = mapId }, FrameLayout.LayoutParams(-1, -1))
        val fragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction().replace(mapId, fragment).commitNow()
        fragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        clusterManager = ClusterManager(this, map)
        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)
        map.setOnMapClickListener {
            selectedLatLng = it
            toast("Selected ${"%.5f".format(it.latitude)}, ${"%.5f".format(it.longitude)}")
            bottomNav.selectedItemId = NAV_TAG
        }
        refreshClusters(latestTags)
        enableLocationIfAllowed()
        val bengaluru = LatLng(12.9716, 77.5946)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(bengaluru, 13f))
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationIfAllowed() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!fine) {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        googleMap?.isMyLocationEnabled = true
        fusedLocation.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lastLocation = location
                selectedLatLng = LatLng(location.latitude, location.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng!!, 17f))
            }
        }
    }

    private fun refreshClusters(tags: List<TreeTag>) {
        val manager = clusterManager ?: return
        manager.clearItems()
        tags.forEach { manager.addItem(TreeClusterItem(it)) }
        manager.cluster()
    }

    private fun showTagger() {
        content.removeAllViews()
        content.addView(scroll {
            addView(header("Tree Tagger"))
            addView(metricCard("Location accuracy", accuracyText(), "Use GPS location or tap the map. Tags are flagged when accuracy is worse than 5m."))
            addView(Button(context).apply {
                text = "Use current GPS location"
                setOnClickListener { useCurrentLocation() }
            }, matchWrap())

            val typeGroup = ChipGroup(context).apply {
                isSingleSelection = true
                addChip("Tree", true)
                addChip("Empty Pit", false)
            }
            addView(typeGroup)

            val speciesSpinner = Spinner(context).apply {
                adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, Species.entries.map { "${it.commonName} / ${it.kannadaName}" })
            }
            val healthSpinner = Spinner(context).apply {
                adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, TreeHealth.entries.map { it.label })
            }
            val girth = EditText(context).apply {
                hint = "Tree girth in cm"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText("80")
            }
            val notes = EditText(context).apply {
                hint = "Street notes, landmark, pit condition"
                minLines = 2
            }
            addView(speciesSpinner, matchWrap())
            addView(healthSpinner, matchWrap())
            addView(girth, matchWrap())
            addView(notes, matchWrap())
            addView(Button(context).apply {
                text = "Add tree photo"
                setOnClickListener { photoLauncher.launch(null) }
            }, matchWrap())
            addView(Button(context).apply {
                text = "Save census tag"
                setOnClickListener {
                    saveTag(typeGroup, speciesSpinner, healthSpinner, girth, notes)
                }
            }, matchWrap())
            addView(Button(context).apply {
                text = "Sync community map"
                setOnClickListener {
                    lifecycleScope.launch {
                        runCatching { sync.pushUnsynced() }
                            .onSuccess { toast("Synced pending tags") }
                            .onFailure { toast("Firebase setup needed: ${it.message}") }
                    }
                }
            }, matchWrap())
        })
    }

    @SuppressLint("MissingPermission")
    private fun useCurrentLocation() {
        fusedLocation.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                toast("Location not available yet")
            } else {
                lastLocation = location
                selectedLatLng = LatLng(location.latitude, location.longitude)
                toast("GPS selected with ${location.accuracy.roundToInt()}m accuracy")
            }
        }
    }

    private fun saveTag(
        typeGroup: ChipGroup,
        speciesSpinner: Spinner,
        healthSpinner: Spinner,
        girth: EditText,
        notes: EditText
    ) {
        val point = selectedLatLng ?: return toast("Pick a location first")
        val type = if (typeGroup.checkedChipId == typeGroup.getChildAt(1).id) TreeType.EMPTY_PIT else TreeType.TREE
        val species = if (type == TreeType.EMPTY_PIT) null else Species.entries[speciesSpinner.selectedItemPosition]
        val simulated = if (capturedPhoto != null && species != null) Species.simulatedFromPhoto("${System.nanoTime()}") else species
        val health = TreeHealth.entries[healthSpinner.selectedItemPosition]
        val accuracy = lastLocation?.accuracy ?: 999f
        if (accuracy > 5f) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Accuracy is ${accuracy.roundToInt()}m")
                .setMessage("Success criteria asks for 5m accuracy. Save anyway for demo data, or move outdoors and try GPS again.")
                .setPositiveButton("Save anyway") { _, _ -> insertTag(type, point, accuracy, simulated, health, girth, notes) }
                .setNegativeButton("Try again", null)
                .show()
        } else {
            insertTag(type, point, accuracy, simulated, health, girth, notes)
        }
    }

    private fun insertTag(
        type: TreeType,
        point: LatLng,
        accuracy: Float,
        species: Species?,
        health: TreeHealth,
        girth: EditText,
        notes: EditText
    ) {
        val tag = TreeTag(
            type = type,
            latitude = point.latitude,
            longitude = point.longitude,
            accuracyMeters = accuracy,
            species = species,
            girthCm = girth.text.toString().toIntOrNull()?.coerceIn(10, 600) ?: 60,
            health = health,
            photoUri = if (capturedPhoto != null) "camera-preview" else null,
            notes = notes.text.toString()
        )
        lifecycleScope.launch {
            dao.insert(tag)
            capturedPhoto = null
            toast("Saved ${if (type == TreeType.EMPTY_PIT) "empty pit" else species?.commonName}")
            bottomNav.selectedItemId = NAV_SCORE
        }
    }

    private fun showScore() {
        content.removeAllViews()
        val trees = latestTags.filter { it.type == TreeType.TREE }
        val pits = latestTags.count { it.type == TreeType.EMPTY_PIT }
        val score = trees.sumOf { it.oxygenScore }
        content.addView(scroll {
            addView(header("Oxygen Score"))
            addView(metricCard("${"%.1f".format(score)} O₂ points", "Dynamic street score", "Mock formula: tree girth x native species factor x health multiplier."))
            addView(metricCard("${trees.size}", "Mapped trees", "Healthy native trees increase the local score."))
            addView(metricCard("$pits", "Empty pits", "These are priority locations for municipal planting drives."))
            latestTags.take(12).forEach { tag ->
                addView(metricCard(
                    title = if (tag.type == TreeType.EMPTY_PIT) "Empty pit" else "${tag.species?.commonName} / ${tag.species?.kannadaName}",
                    subtitle = "${"%.5f".format(tag.latitude)}, ${"%.5f".format(tag.longitude)}",
                    detail = "Health: ${tag.health.label}  Score: ${"%.1f".format(tag.oxygenScore)}"
                ))
            }
        })
    }

    private fun showGuide() {
        content.removeAllViews()
        content.addView(scroll {
            addView(header("Species Guide"))
            Species.entries.forEach { species ->
                addView(MaterialCardView(context).apply {
                    radius = 8f
                    cardElevation = 2f
                    useCompatPadding = true
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(18, 18, 18, 18)
                        addView(ImageView(context).apply {
                            setImageResource(species.imageRes)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }, LinearLayout.LayoutParams(-1, 320))
                        addView(TextView(context).apply {
                            text = "${species.commonName} / ${species.kannadaName}"
                            textSize = 21f
                            setTextColor(color(R.color.leaf_dark))
                            setPadding(0, 12, 0, 0)
                        })
                        addView(TextView(context).apply { text = species.scientificName })
                        addView(TextView(context).apply { text = "Soil: ${species.soil}" })
                        addView(TextView(context).apply {
                            text = species.descriptionKannada
                            textSize = 16f
                            setPadding(0, 8, 0, 0)
                        })
                    })
                }, matchWrap())
            }
        })
    }

    private fun scroll(build: LinearLayout.() -> Unit): ScrollView =
        ScrollView(this).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
                build()
            })
        }

    private fun ChipGroup.addChip(textValue: String, checked: Boolean) {
        addView(Chip(context).apply {
            id = View.generateViewId()
            text = textValue
            isCheckable = true
            isChecked = checked
        })
    }

    private fun header(textValue: String): TextView = TextView(this).apply {
        text = textValue
        textSize = 28f
        setTextColor(color(R.color.leaf_dark))
        setPadding(0, 0, 0, 16)
    }

    private fun metricCard(title: String, subtitle: String, detail: String): MaterialCardView =
        MaterialCardView(this).apply {
            radius = 8f
            cardElevation = 2f
            useCompatPadding = true
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 18, 20, 18)
                addView(TextView(context).apply {
                    text = title
                    textSize = 24f
                    setTextColor(color(R.color.leaf_dark))
                })
                addView(TextView(context).apply {
                    text = subtitle
                    textSize = 16f
                    setTextColor(color(R.color.soil))
                })
                addView(TextView(context).apply {
                    text = detail
                    setPadding(0, 6, 0, 0)
                })
            })
        }

    private fun accuracyText(): String {
        val accuracy = lastLocation?.accuracy
        return if (accuracy == null) "Waiting for GPS" else "${accuracy.roundToInt()}m"
    }

    private fun color(id: Int): Int = ContextCompat.getColor(this, id)
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 8, 0, 8) }

    companion object {
        private const val NAV_MAP = 1
        private const val NAV_TAG = 2
        private const val NAV_SCORE = 3
        private const val NAV_GUIDE = 4
    }
}
