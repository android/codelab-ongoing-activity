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
package com.android.example.wear.ongoingactivity.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

internal val wearColorPalette: Colors = Colors(
    primary = Color(0x0, 0xBC, 0xD4, 0xFF),
    secondary = Color(0xE9, 0x1E, 0x63, 0xFF),
    surface = Color(0x40, 0x40, 0x40, 0xFF),
    error = Color.Red,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onError = Color.Black,
    onSurface = Color(0xFF, 0xFF, 0xFF, 0xFF),
)

@Composable
fun OngoingActivityExampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = wearColorPalette,
        content = content,
    )
}
