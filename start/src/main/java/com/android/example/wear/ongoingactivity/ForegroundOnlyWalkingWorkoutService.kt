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
package com.android.example.wear.ongoingactivity

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.android.example.wear.ongoingactivity.data.WalkingWorkoutsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Service calculates "Walking Points" from mock Location and Sensor data for active walking
 * workouts.
 *
 * When [MainActivity] is visible it binds to this Service to start/stop walking workouts.
 *
 * If an walking workout is active and the user navigates away from [MainActivity], the service
 * will promote itself to a Foreground Service with an Ongoing Notification, so the user continues
 * to track "Walking Points" and gets updates.
 *
 * In the code lab, you will add an Ongoing Activity to the Ongoing Notification, so you can
 * see the data in more surfaces (watch face and recent section of app launcher).
 *
 * This design (Service handling location and sensor callbacks to calculate data) is a good
 * design for "while-in-use" location changes (only require "while-in-use" location permissions).
 * This uses mock data, so you don't need any of the permission code.
 *
 * To learn more about Wear or Location, seen the links below:
 *
 * @see <a href="https://developer.android.com/wear">Wear guides</a>.
 * @see <a href="https://developer.android.com/training/location">Location guides</a>.
 */
class ForegroundOnlyWalkingWorkoutService : LifecycleService() {
    private val walkingWorkoutsRepository: WalkingWorkoutsRepository by lazy {
        (application as MainApplication).repository
    }

    private lateinit var notificationManager: NotificationManager

    /*
     * Checks whether the bound activity has really gone away (in which case a foreground service
     * with notification is created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private var walkingWorkoutActive = false

    // Created by coroutine to track mock location and sensor data for "Walking Points". If the
    // walking workout is cancelled, we need to cancel the work.
    private var mockDataForWalkingWorkoutJob: Job? = null

    private fun setActiveWalkingWorkout(active: Boolean) = lifecycleScope.launch {
        walkingWorkoutsRepository.setActiveWalkingWorkout(active)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        lifecycleScope.launch {
            walkingWorkoutsRepository.activeWalkingWorkoutFlow.collect { isActive ->
                if (walkingWorkoutActive != isActive) {
                    walkingWorkoutActive = isActive
                }
            }
        }

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand()")

        val cancelWorkoutFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_WORKOUT_FROM_NOTIFICATION, false)
                ?: false

        if (cancelWorkoutFromNotification) {
            stopWalkingWorkoutWithServiceShutdownOption(stopService = true)
        }
        // Tells the system not to recreate the service after it's been killed.
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Log.d(TAG, "onBind()")

        // MainActivity (client) comes into foreground and binds to service, so the service can
        // move itself back to a background services.
        notForegroundService()
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
        Log.d(TAG, "onRebind()")

        // MainActivity (client) comes into foreground and binds to service, so the service can
        // move itself back to a background services.
        notForegroundService()
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")

        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label (usually needed for location services).
        // NOTE: If this method is called due to a configuration change in MainActivity,
        // we do nothing.
        if (!configurationChange && walkingWorkoutActive) {
            Log.d(TAG, "Start foreground service")
            val notification =
                generateNotification(getString(R.string.walking_workout_notification_started_text))
            // startForeground takes care of notificationManager.notify(...).
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    private fun notForegroundService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceRunningInForeground = false
        configurationChange = false
    }

    fun startWalkingWorkout() {
        Log.d(TAG, "startWalkingWorkout()")

        setActiveWalkingWorkout(true)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, ForegroundOnlyWalkingWorkoutService::class.java))

        // Normally, you would subscribe to location and sensor callbacks here, but since this
        // is a simplified example to teach Ongoing Activities, we are mocking the data.
        mockDataForWalkingWorkoutJob = lifecycleScope.launch {
            mockSensorAndLocationForWalkingWorkout()
        }
    }

    fun stopWalkingWorkout() {
        Log.d(TAG, "stopWalkingWorkout()")
        stopWalkingWorkoutWithServiceShutdownOption(false)
    }

    /**
     * Stops workout with extra ability to shut down the Service.
     *
     * This is needed if the user cancels the walking workout from a notification. Because the
     * walking workout status is set via coroutines to our DataStore, we need to wait until that is
     * complete before shutting down the service. Otherwise, the data won't be saved.
     */
    private fun stopWalkingWorkoutWithServiceShutdownOption(stopService: Boolean) {
        Log.d(TAG, "stopWalkingWorkout()")
        mockDataForWalkingWorkoutJob?.cancel()

        lifecycleScope.launch {
            val job: Job = setActiveWalkingWorkout(false)
            if (stopService) {
                // Waits until DataStore data is saved before shutting down service.
                job.join()
                stopSelf()
            }
        }
    }

    // Normally, you would listen to the location and sensor data and calculate your points with
    // an algorithm, but we are mocking the data to simply this so we can focus on learning about
    // the Ongoing Activity API.
    private suspend fun mockSensorAndLocationForWalkingWorkout() {
        for (walkingPoints in 0 until 100) {
            if (serviceRunningInForeground) {
                val notification = generateNotification(
                    getString(R.string.walking_points_text, walkingPoints),
                )
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "mockSensorAndLocationForWalkingWorkout(): $walkingPoints")
            walkingWorkoutsRepository.setWalkingPoints(walkingPoints)
            delay(THREE_SECONDS_MILLISECONDS)
        }
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest Walking Points while a
     * workout is active.
     */
    private fun generateNotification(mainText: String): Notification {
        Log.d(TAG, "generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data (note, the main notification text comes from the parameter above).
        val titleText = getString(R.string.notification_title)

        // 1. Create Notification Channel.
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            titleText,
            NotificationManager.IMPORTANCE_DEFAULT,
        )

        // Adds NotificationChannel to system. Attempting to create an
        // existing notification channel with its original values performs
        // no operation, so it's safe to perform the below sequence.
        notificationManager.createNotificationChannel(notificationChannel)

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, ForegroundOnlyWalkingWorkoutService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_WORKOUT_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchActivityIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        // 4. Build and issue the notification.
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        // TODO: Review Notification builder code.
        val notificationBuilder = notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // Makes Notification an Ongoing Notification (a Notification with a background task).
            .setOngoing(true)
            // For an Ongoing Activity, used to decide priority on the watch face.
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_walk,
                getString(R.string.launch_activity),
                activityPendingIntent,
            )
            .addAction(
                R.drawable.ic_cancel,
                getString(R.string.stop_walking_workout_notification_text),
                servicePendingIntent,
            )

        // TODO: Create an Ongoing Activity.
        // SKIP TODO FOR REVIEW STEP

        return notificationBuilder.build()
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val walkingWorkoutService: ForegroundOnlyWalkingWorkoutService
            get() = this@ForegroundOnlyWalkingWorkoutService
    }

    companion object {
        private const val TAG = "ForegroundOnlyService"

        private const val THREE_SECONDS_MILLISECONDS = 3000L

        private const val PACKAGE_NAME = "com.android.example.wear.ongoingactivity"

        private const val EXTRA_CANCEL_WORKOUT_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_SUBSCRIPTION_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "walking_workout_channel_01"
    }
}
