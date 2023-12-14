/*
 * Copyright 2021 The Android Open Source Project
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
import kotlinx.coroutines.flow.Flow

/**
 * Access point for Walking Workout data.
 */
class WalkingWorkoutsRepository private constructor(
    private val walkingWorkoutsDataStore: WalkingWorkoutsDataStore,
) {
    val activeWalkingWorkoutFlow: Flow<Boolean> = walkingWorkoutsDataStore.activeWalkingWorkoutFlow

    suspend fun setActiveWalkingWorkout(activeWorkout: Boolean) =
        walkingWorkoutsDataStore.setActiveWalkingWorkout(activeWorkout)

    val walkingPointsFlow: Flow<Int> = walkingWorkoutsDataStore.walkingPointsFlow

    suspend fun setWalkingPoints(points: Int) = walkingWorkoutsDataStore.setWalkingPoints(points)

    companion object {
        @Volatile private var INSTANCE: WalkingWorkoutsRepository? = null

        fun getInstance(context: Context): WalkingWorkoutsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WalkingWorkoutsRepository(
                    WalkingWorkoutsDataStore(context),
                )
                    .also { INSTANCE = it }
            }
        }
    }
}
