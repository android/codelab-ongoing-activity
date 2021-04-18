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
package com.android.example.wear.ongoingactivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.android.example.wear.ongoingactivity.data.WalkingWorkoutsRepository

/**
 * Allows Activity to observe [WalkingWorkoutsDataStore] data: walking workout state and points.
 */
class MainViewModel(walkingWorkoutsRepository: WalkingWorkoutsRepository) : ViewModel() {
    val activeWalkingWorkoutFlow = walkingWorkoutsRepository.activeWalkingWorkoutFlow.asLiveData()
    val walkingPointsFlow = walkingWorkoutsRepository.walkingPointsFlow.asLiveData()
}

class MainViewModelFactory(
    private val walkingWorkoutsRepository: WalkingWorkoutsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(walkingWorkoutsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
