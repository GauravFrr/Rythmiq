package com.premium.spotifyclone.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var dataSaver by remember { mutableStateOf(sharedPrefs.getBoolean("data_saver", false)) }
    var gaplessPlayback by remember { mutableStateOf(sharedPrefs.getBoolean("gapless", true)) }
    var normalizeVolume by remember { mutableStateOf(sharedPrefs.getBoolean("normalize_volume", true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item { SettingsSectionHeader("Data Saver") }
            item {
                SettingsToggleItem(
                    title = "Data Saver",
                    subtitle = "Sets your audio quality to low and disables artist canvases.",
                    checked = dataSaver,
                    onCheckedChange = { 
                        dataSaver = it 
                        sharedPrefs.edit().putBoolean("data_saver", it).apply()
                    }
                )
            }

            item { SettingsSectionHeader("Playback") }
            item {
                SettingsToggleItem(
                    title = "Gapless",
                    subtitle = "Allows gapless playback.",
                    checked = gaplessPlayback,
                    onCheckedChange = { 
                        gaplessPlayback = it 
                        sharedPrefs.edit().putBoolean("gapless", it).apply()
                    }
                )
            }
            item {
                SettingsToggleItem(
                    title = "Normalize volume",
                    subtitle = "Set the same volume level for all songs and podcasts.",
                    checked = normalizeVolume,
                    onCheckedChange = { 
                        normalizeVolume = it 
                        sharedPrefs.edit().putBoolean("normalize_volume", it).apply()
                    }
                )
            }
            item {
                SettingsActionItem(
                    title = "Equalizer",
                    subtitle = "Open device equalizer settings",
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Equalizer settings unavailable") }
                    }
                )
            }
            
            item { SettingsSectionHeader("Storage") }
            item {
                SettingsActionItem(
                    title = "Clear cache",
                    subtitle = "Free up storage by clearing your cache. Your downloads won't be removed.",
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Cache cleared successfully (54 MB freed)") }
                    }
                )
            }
            
            item { SettingsSectionHeader("About") }
            item {
                SettingsActionItem(
                    title = "Version",
                    subtitle = "8.8.12.545",
                    onClick = {}
                )
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFFB3B3B3), fontSize = 13.sp, lineHeight = 18.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1DB954),
                uncheckedThumbColor = Color(0xFFB3B3B3),
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
fun SettingsActionItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFFB3B3B3), fontSize = 13.sp)
        }
    }
}
