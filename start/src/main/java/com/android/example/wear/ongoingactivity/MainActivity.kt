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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.android.example.wear.ongoingactivity.databinding.ActivityMainBinding

/**
 * The app's main purpose is to teach developers how to use Wear's Ongoing Activity API via the
 * @see <a href="https://codelabs.developers.google.com/codelabs/ongoing-activity/">
 * Wear Ongoing Activity Code Lab</a>.
 *
 * An Ongoing Activity allows an Ongoing Notification to appear on more Wear surfaces, for example,
 * the activity indicator on the watch face or Recent apps in the app launcher. Users can then
 * easily re-enter the app through a simple tap from those surfaces.
 *
 * Ongoing Notifications are typically used to indicate a Notification has a background task that
 * the user is actively engaged with (e.g., playing music) or is pending in some way and therefore
 * occupying the device (e.g., a file download, sync operation, active network connection).
 *
 * For this app, we will use the Ongoing Notification to continue gathering (mock) sensor/location
 * data if the user navigates away from this Activity during an active walking workout.
 *
 * This Activity allows a user to start and stop a walking workout and earn "walking points" from
 * the UI. The actual work to start and stop a walking workout is done in the
 * [ForegroundOnlyWalkingWorkoutService] this Activity binds to. That Service creates the
 * walking points from mock location and sensor data.
 *
 * When a workout is active and the user navigates away from this Activity, that bound service is
 * promoted to a Foreground Service and tied to an Ongoing Notification, so the user can continue to
 * receive updates and this app can continue to gather location and sensor data to calculate
 * "walking points".
 *
 * By the end of this code lab, you will have exposed the same data from the Notification to new
 * surfaces when the user navigates away from an active workout from this Activity.
 *
 * NOTE: You should generally prefer 'while-in-use' for location updates, i.e., receiving
 * location updates while the app is in use and create a foreground service (tied to a Notification)
 * when the user navigates away from the app. This is why we are using this approach in this code
 * lab. For more information, review the
 * @see <a href="https://developer.android.com/training/location">location guides</a>.
 */
class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as MainApplication).repository)
    }

    // Status of the walking workout. Only updated when different from current value and pulled from
    // the WalkingWorkoutDataStore via the repository.
    private var activeWalkingWorkout = false
        set(newActiveStatus) {
            if (field != newActiveStatus) {
                field = newActiveStatus
                if (newActiveStatus) {
                    binding.startStopWalkingWorkoutButton.text =
                        getString(R.string.stop_walking_workout_button_text)
                } else {
                    binding.startStopWalkingWorkoutButton.text =
                        getString(R.string.start_walking_workout_button_text)
                }
                updateOutput(walkingPoints)
            }
        }

    private var walkingPoints = 0

    // The remaining variables are related to the binding/monitoring/interacting with the
    // service that gathers all the data to calculate walking points.
    private var foregroundOnlyServiceBound = false

    // Gathers mock location/sensor data for walking workouts and also promotes itself to a
    // foreground service with Ongoing Notification to continue gathering data when a user is
    // engaged in an active walking workout.
    private var foregroundOnlyWalkingWorkoutService: ForegroundOnlyWalkingWorkoutService? = null

    // Monitors connection to the service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyWalkingWorkoutService.LocalBinder
            foregroundOnlyWalkingWorkoutService = binder.walkingWorkoutService
            foregroundOnlyServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyWalkingWorkoutService = null
            foregroundOnlyServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainViewModel.walkingPointsFlow.observe(this) { points ->
            walkingPoints = points
            updateOutput(walkingPoints)
        }

        mainViewModel.activeWalkingWorkoutFlow.observe(this) { active ->
            Log.d(TAG, "Workout Status changed: $activeWalkingWorkout")
            activeWalkingWorkout = active
        }
    }

    override fun onStart() {
        super.onStart()

        val serviceIntent = Intent(this, ForegroundOnlyWalkingWorkoutService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        if (foregroundOnlyServiceBound) {
            unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyServiceBound = false
        }
        super.onStop()
    }

    fun onClickWalkingWorkout(view: View) {
        Log.d(TAG, "onClickWalkingWorkout()")
        if (activeWalkingWorkout) {
            foregroundOnlyWalkingWorkoutService?.stopWalkingWorkout()
        } else {
            foregroundOnlyWalkingWorkoutService?.startWalkingWorkout()
        }
    }

    private fun updateOutput(points: Int) {
        Log.d(TAG, "updateOutput()")
        val output = getString(R.string.walking_points_text, points)
        binding.outputTextView.text = output
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
