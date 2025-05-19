package com.example.pruebasnfc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pruebasnfc.SessionManager.getSession
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            var startDestination by remember { mutableStateOf("login") }

            LaunchedEffect(true) {
                val hasSession = getSession(context)
                if (hasSession) startDestination = "dashboard"
            }

            NavHost(navController, startDestination = startDestination) {
                composable("login") { LoginScreen(navController) }
                composable("dashboard") { DashboardScreen(navController) }
            }
        }
    }
}

@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberSession by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Iniciar Sesión", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Checkbox(checked = rememberSession, onCheckedChange = { rememberSession = it })
            Text("Recordar sesión")
        }

        Button(onClick = {
            //Muy seguro
            if (email == "admin@admin.cl" && password == "123456") {
                coroutineScope.launch {
                    SessionManager.saveSession(context, rememberSession)
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            } else {
                errorMessage = "Credenciales inválidas"
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Ingresar")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
@Composable
fun DashboardScreen(navController: NavHostController) {
    var selectedIndex by remember { mutableStateOf(0) }

    val items = listOf(
        BottomNavItem("Inicio", Icons.Default.Home),
        BottomNavItem("Perfil", Icons.Default.Person),
        BottomNavItem("Ajustes", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedIndex) {
                0 -> HomeScreen()
                1 -> ProfileScreen()
                2 -> SettingsScreen(navController)
            }
        }
    }
}

@Composable
fun HomeScreen(
    context: Context = LocalContext.current,
    @SuppressLint("ContextCastToActivity") activity: ComponentActivity = LocalContext.current as ComponentActivity
) {
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var locationText by remember { mutableStateOf("Ubicación no disponible") }
    var nfcText by remember { mutableStateOf("Esperando etiqueta NFC...") }

    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    // Permisos ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        locationText = "Lat: ${it.latitude}, Lon: ${it.longitude}"
                    } ?: run {
                        locationText = "No se pudo obtener ubicación"
                    }
                }
            } else {
                locationText = "Permiso denegado"
            }
        }
    )

    // NFC foreground dispatch setup
    DisposableEffect(Unit) {
        val intent = Intent(context, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        val techLists = arrayOf<Array<String>>()

        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, filters, techLists)

        onDispose {
            nfcAdapter?.disableForegroundDispatch(activity)
        }
    }

    // Procesa el intent NFC recibido desde la actividad
    LaunchedEffect(Unit) {
        val intent = (context as? Activity)?.intent
        if (intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.id?.let {
                val idHex = it.joinToString("") { byte -> "%02X".format(byte) }
                nfcText = "Tag leída: $idHex"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                when {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                locationText = "Lat: ${it.latitude}, Lon: ${it.longitude}"
                            } ?: run {
                                locationText = "No se pudo obtener ubicación"
                            }
                        }
                    }
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) -> {
                        locationText = "Se requiere permiso de ubicación para continuar"
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    else -> {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }) {
                Text("Obtener ubicación")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(locationText)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Lectura NFC:")
            Text(nfcText)
        }
    }
}

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Perfil del Usuario", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
fun SettingsScreen(navController: NavHostController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text("🔔 Notificaciones")
        Switch(checked = true, onCheckedChange = {})

        Spacer(modifier = Modifier.height(16.dp))

        Text("🌗 Tema oscuro")
        Switch(checked = false, onCheckedChange = {})

        Spacer(modifier = Modifier.height(16.dp))

        Text("🔒 Cambiar contraseña")
        Button(onClick = { /* Acción futura */ }) {
            Text("Cambiar")
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    SessionManager.clearSession(context)
                    navController.popBackStack("login", inclusive = true)
                    navController.navigate("login")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Salir", color = MaterialTheme.colorScheme.onError)
        }

    }
}




data class BottomNavItem(val label: String, val icon: ImageVector)
