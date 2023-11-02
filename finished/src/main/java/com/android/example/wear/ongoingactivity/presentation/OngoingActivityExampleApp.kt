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
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.example.wear.ongoingactivity.MainViewModel
import com.android.example.wear.ongoingactivity.MainViewModelFactory
import com.android.example.wear.ongoingactivity.data.WalkingWorkoutsRepository
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OngoingActivityExampleApp(
    repository: WalkingWorkoutsRepository,
    onStartStopClick: (Boolean) -> Unit,
) {
    // On T and upwards, POST_NOTIFICATIONS must be requested at runtime.
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    if (permissionState?.status == PermissionStatus.Granted ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
        val viewModel: MainViewModel = viewModel(
            factory = MainViewModelFactory(repository),
        )
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
    } else {
        PermissionRequiredScreen(
            onPermissionClick = { permissionState?.launchPermissionRequest() },
        )
    }
}
