/*
 * Copyright 2025 The Android Open Source Project
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
package com.android.example.wear.ongoingactivity.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PermissionStateDataStore(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        "permission_state_data_store",
    )

    /**
     * Returns whether the rationale for a permission has been shown to the user before.
     */
    val hasPreviouslyShownRationaleFlow: Flow<ShownRationaleStatus> =
        context.dataStore.data
            .map { preferences ->
                when (preferences[HAS_SHOWN_RATIONALE_KEY]) {
                    true -> ShownRationaleStatus.HAS_SHOWN
                    else -> ShownRationaleStatus.HAS_NOT_SHOWN
                }
            }

    /**
     * Updates whether the rationale for a permission has been shown to the user before.
     */
    suspend fun setHasPreviouslyShownRationale(hasShownRationale: ShownRationaleStatus) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SHOWN_RATIONALE_KEY] =
                hasShownRationale == ShownRationaleStatus.HAS_SHOWN
        }
    }
    companion object {
        private val HAS_SHOWN_RATIONALE_KEY = booleanPreferencesKey("has_shown_rationale")
    }
}

enum class ShownRationaleStatus {
    HAS_SHOWN,
    HAS_NOT_SHOWN,
    UNKNOWN,
}
