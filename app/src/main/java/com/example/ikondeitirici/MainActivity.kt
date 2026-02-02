package com.example.ikondeitirici

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.ikondeitirici.ui.theme.IkonDeğiştiriciTheme
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IkonDeğiştiriciTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    var isUnlocked by remember { mutableStateOf(!prefs.contains("app_password")) }
                    
                    if (isUnlocked) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            IconChangerScreen(modifier = Modifier.padding(innerPadding))
                        }
                    } else {
                        PasswordScreen(onSuccess = { isUnlocked = true })
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    val savedPassword = prefs.getString("app_password", "") ?: ""
    var passwordInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Giriş Yap", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (passwordInput == savedPassword) {
                    onSuccess()
                } else {
                    Toast.makeText(context, "Hatalı Şifre!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Giriş")
        }
    }
}

@Composable
fun IconChangerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("İkon Değiştir", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Ayarlar", modifier = Modifier.padding(16.dp))
            }
        }

        when (selectedTab) {
            0 -> MainChangerContent(context)
            1 -> SettingsScreen(context)
        }
    }
}

@Composable
fun MainChangerContent(context: Context) {
    var subTab by remember { mutableIntStateOf(0) }
    Column {
        ScrollableTabRow(selectedTabIndex = subTab, edgePadding = 0.dp) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }) { Text("Bu Uygulama", modifier = Modifier.padding(12.dp)) }
            Tab(selected = subTab == 1, onClick = { subTab = 1 }) { Text("Diğer Uygulamalar", modifier = Modifier.padding(12.dp)) }
        }
        if (subTab == 0) SelfIconChanger(context) else AppShortcutCreator(context)
    }
}

@Composable
fun SettingsScreen(context: Context) {
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    var newPassword by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Uygulama Şifresi Belirle", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("Yeni Şifre") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                prefs.edit().putString("app_password", newPassword).apply()
                Toast.makeText(context, "Şifre Kaydedildi!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Şifreyi Kaydet")
        }
    }
}

@Composable
fun SelfIconChanger(context: Context) {
    val iconOptions = listOf(
        IconOption("Varsayılan", "com.example.ikondeitirici.MainActivity"),
        IconOption("Hava Durumu", "com.example.ikondeitirici.MainActivityWeather"),
        IconOption("Hesap Makinesi", "com.example.ikondeitirici.MainActivityCalculator")
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Uygulama İkonunu Maskele", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(iconOptions) { option ->
                Button(
                    onClick = { changeAppIcon(context, option.className) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text(option.name) }
            }
        }
    }
}

@Composable
fun AppShortcutCreator(context: Context) {
    val packageManager = context.packageManager
    var installedApps by remember { mutableStateOf(listOf<ApplicationInfo>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<ApplicationInfo?>(null) }
    var customName by remember { mutableStateOf("") }
    var customBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            customBitmap = BitmapFactory.decodeStream(inputStream)
        }
    }

    LaunchedEffect(Unit) {
        installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || it.packageName.contains("calculator") }
            .sortedBy { it.loadLabel(packageManager).toString() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (selectedApp == null) {
            TextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Uygulama Seçin") }, modifier = Modifier.fillMaxWidth())
            LazyColumn {
                val filteredApps = installedApps.filter { it.loadLabel(packageManager).toString().contains(searchQuery, ignoreCase = true) }
                items(filteredApps) { app ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedApp = app; customName = app.loadLabel(packageManager).toString() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(bitmap = drawableToBitmap(app.loadIcon(packageManager)).asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(app.loadLabel(packageManager).toString())
                    }
                }
            }
        } else {
            Text("Simgeyi Değiştir: ${selectedApp?.loadLabel(packageManager)}", style = MaterialTheme.typography.titleLarge)
            TextField(value = customName, onValueChange = { customName = it }, label = { Text("Yeni Ad") }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { createShortcut(context, selectedApp!!, customName, R.drawable.ic_weather) }) { Icon(painterResource(id = R.drawable.ic_weather), "Hava", modifier = Modifier.size(48.dp)) }
                IconButton(onClick = { createShortcut(context, selectedApp!!, customName, R.drawable.ic_calculator) }) { Icon(painterResource(id = R.drawable.ic_calculator), "Hesap", modifier = Modifier.size(48.dp)) }
                Button(onClick = { photoPickerLauncher.launch("image/*") }) { Text("Galeri") }
            }
            customBitmap?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally), contentScale = ContentScale.Crop)
                Button(onClick = { createShortcut(context, selectedApp!!, customName, bitmap = it) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Tamam") }
            }
            Button(onClick = { selectedApp = null; customBitmap = null }, modifier = Modifier.padding(top = 16.dp)) { Text("Geri") }
        }
    }
}

data class IconOption(val name: String, val className: String)

fun changeAppIcon(context: Context, activeClassName: String) {
    val packageManager = context.packageManager
    val packageName = context.packageName
    val configs = listOf("com.example.ikondeitirici.MainActivity", "com.example.ikondeitirici.MainActivityWeather", "com.example.ikondeitirici.MainActivityCalculator")
    configs.forEach { className ->
        val state = if (className == activeClassName) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        packageManager.setComponentEnabledSetting(ComponentName(packageName, className), state, PackageManager.DONT_KILL_APP)
    }
    Toast.makeText(context, "İkon Değişti!", Toast.LENGTH_LONG).show()
}

fun createShortcut(context: Context, app: ApplicationInfo, name: String, iconRes: Int? = null, bitmap: Bitmap? = null) {
    if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        // Hedef uygulamayı açacak olan ProxyActivity'yi başlatan bir intent oluşturuyoruz
        val proxyIntent = Intent(context, ShortcutProxyActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("target_package", app.packageName)
            // Kısayolun her zaman yeni bir görev gibi davranması için
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val icon = when {
            bitmap != null -> IconCompat.createWithBitmap(bitmap)
            iconRes != null -> IconCompat.createWithResource(context, iconRes)
            else -> IconCompat.createWithBitmap(drawableToBitmap(app.loadIcon(context.packageManager)))
        }

        val pinShortcutInfo = ShortcutInfoCompat.Builder(context, app.packageName + System.currentTimeMillis())
            .setShortLabel(name)
            .setIcon(icon)
            .setIntent(proxyIntent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
        Toast.makeText(context, "Kısayol oluşturuluyor...", Toast.LENGTH_SHORT).show()
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) return drawable.bitmap
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
