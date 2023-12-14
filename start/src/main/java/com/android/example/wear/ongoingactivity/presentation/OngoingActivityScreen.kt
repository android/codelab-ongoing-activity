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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.android.example.wear.ongoingactivity.R
import com.android.example.wear.ongoingactivity.theme.OngoingActivityExampleTheme

@Composable
fun OngoingActivityScreen(
    isWalkingActive: Boolean,
    walkingPoints: Int,
    onStartStopClick: (Boolean) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = stringResource(id = R.string.walking_points_text, walkingPoints),
        )
        Button(
            onClick = { onStartStopClick(isWalkingActive) },
            modifier = Modifier.fillMaxWidth(0.8f),
        ) {
            val label = if (isWalkingActive) {
                stringResource(R.string.stop_walking_workout_button_text)
            } else {
                stringResource(R.string.start_walking_workout_button_text)
            }
            Text(
                text = label,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@WearPreviewDevices
@Composable
fun MainScreenPreview() {
    OngoingActivityExampleTheme {
        OngoingActivityScreen(
            isWalkingActive = true,
            walkingPoints = 1000,
            onStartStopClick = {},
        )
    }
}
