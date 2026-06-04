package com.danzucker.networklocationtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.danzucker.networklocationtracker.core.service.NetworkTrackerService
import com.danzucker.networklocationtracker.networktrackerlist.NetworkTrackerRoot
import com.danzucker.networklocationtracker.ui.theme.NetworkLocationTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetworkLocationTrackerTheme {
                AppHost()
            }
        }
    }

    private fun startTrackerService() {
        ContextCompat.startForegroundService(this, NetworkTrackerService.startIntent(this))
    }

    private fun stopTrackerService() {
        startService(NetworkTrackerService.stopIntent(this))
    }

    @Composable
    private fun AppHost() {
        var granted by remember { mutableStateOf(hasRequiredPermissions()) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            granted = REQUIRED_PERMISSIONS.all { result[it] == true }
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            if (granted) {
                NetworkTrackerRoot(
                    onStartService = ::startTrackerService,
                    onStopService = ::stopTrackerService,
                )
            } else {
                PermissionsGate(
                    modifier = Modifier.padding(innerPadding),
                    onRequest = { launcher.launch(REQUIRED_PERMISSIONS) },
                )
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean =
        REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

    companion object {
        private val REQUIRED_PERMISSIONS: Array<String> = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
}

@Composable
private fun PermissionsGate(
    modifier: Modifier = Modifier,
    onRequest: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.permissions_required_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.permissions_required_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text(stringResource(R.string.permissions_required_retry))
        }
    }
}
