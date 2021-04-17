/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Tracks walking workouts and points associated with them. This is a simple implementation to teach
 * Wear's Ongoing Activity API, so it only tracks one workout (ever).
 */
class WalkingWorkoutsDataStore(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = WALKING_WORKOUTS_DATASTORE_NAME
    )

    val activeWalkingWorkoutFlow: Flow<Boolean> = context.dataStore.data.map {
        it[ACTIVE_WALKING_WORKOUT_KEY] ?: false
    }

    suspend fun setActiveWalkingWorkout(activeWalkingWorkout: Boolean) {
        context.dataStore.edit {
            it[ACTIVE_WALKING_WORKOUT_KEY] = activeWalkingWorkout
        }
    }

    val walkingPointsFlow: Flow<Int> = context.dataStore.data.map {
        it[WALKING_POINTS_KEY] ?: 0
    }

    suspend fun setWalkingPoints(walkingPoints: Int) {
        context.dataStore.edit {
            it[WALKING_POINTS_KEY] = walkingPoints
        }
    }

    companion object {
        private const val WALKING_WORKOUTS_DATASTORE_NAME = "walking_workouts_datastore"

        private val WALKING_POINTS_KEY = intPreferencesKey("walking_points")
        private val ACTIVE_WALKING_WORKOUT_KEY =
            booleanPreferencesKey("active_walking_workout")
    }
}
