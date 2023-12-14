/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.example.wear.ongoingactivity.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.example.wear.ongoingactivity.MainViewModel
import com.android.example.wear.ongoingactivity.MainViewModelFactory
import com.android.example.wear.ongoingactivity.R
import com.android.example.wear.ongoingactivity.data.PermissionStateDataStore
import com.android.example.wear.ongoingactivity.data.ShownRationaleStatus
import com.android.example.wear.ongoingactivity.data.WalkingWorkoutsRepository
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OngoingActivityExampleApp(
    repository: WalkingWorkoutsRepository,
    permissionStateDataStore: PermissionStateDataStore,
    onStartStopClick: (Boolean) -> Unit,
) {
    // On T and upwards, POST_NOTIFICATIONS must be requested at runtime.
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        // Below T, POST_NOTIFICATIONS does not need to be requested at runtime but must still be
        // specified in the Manifest. Therefore, permissionState is created such that it is already
        // in the granted state.
        object: PermissionState {
            override val permission = "no_runtime_permission_required"
            override val status = PermissionStatus.Granted
            override fun launchPermissionRequest() { }
        }
    }
    val context = LocalContext.current

    if (permissionState.status == PermissionStatus.Granted) {
        val viewModel: MainViewModel = viewModel(
            factory = MainViewModelFactory(repository),
        )
        LaunchedEffect(Unit) {
            // Reset the status of having shown permission rationale.
            permissionStateDataStore.setHasPreviouslyShownRationale(ShownRationaleStatus.UNKNOWN)
        }
        val isWalkingActive by viewModel.activeWalkingWorkoutFlow.collectAsStateWithLifecycle(
            initialValue = false,
        )
        val walkingPoints by viewModel.walkingPointsFlow.collectAsStateWithLifecycle(
            initialValue = 0,
        )
        OngoingActivityScreen(
            isWalkingActive = isWalkingActive,
            walkingPoints = walkingPoints,
            onStartStopClick = onStartStopClick,
        )
    } else if (permissionState.status is PermissionStatus.Denied) {
        val denied = permissionState.status as PermissionStatus.Denied
        val hasPreviouslyShown by permissionStateDataStore
            .hasPreviouslyShownRationaleFlow
            .collectAsStateWithLifecycle(initialValue = ShownRationaleStatus.UNKNOWN)

        if (denied.shouldShowRationale) {
            LaunchedEffect(Unit) {
                permissionStateDataStore.setHasPreviouslyShownRationale(ShownRationaleStatus.HAS_SHOWN)
            }
            // ShouldShowRationale returns true if:
            // - A request has previously been denied
            // - The app permission was set to denied in settings
            // At this point, the app stores the state that the rationale has been shown, as if
            // subsequently false is returned, this means that the permission cannot be requested
            // now, as opposed to the false seen from shouldShowRationale on first ever launch
            PermissionRequiredScreen(
                onPermissionClick = { permissionState.launchPermissionRequest() },
                buttonLabelResId = R.string.show_permissions
            )
        } else if (hasPreviouslyShown == ShownRationaleStatus.HAS_SHOWN) {
            // Rationale has been shown previously, but the user has decided not to grant permission
            // Offer the user the option to go to permission settings.
            PermissionRequiredScreen(
                onPermissionClick = { launchPermissionsSettings(context) },
                buttonLabelResId = R.string.show_settings
            )
        } else if (hasPreviouslyShown == ShownRationaleStatus.HAS_NOT_SHOWN) {
            // First launch of permissions, show the permission request without any rationale.
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}

private fun launchPermissionsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.setData(uri)
    context.startActivity(intent)
}
