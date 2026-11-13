package ca.bcparkspot.app

import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper

/** JVM tests run against the local Firebase emulators and a test WorkManager, never the real project. */
class TestBCParkSpotApplication : BCParkSpotApplication() {
    override fun onCreate() {
        AppGraph.resetForTests()
        AppGraph.emulatorHost = TEST_EMULATOR_HOST
        WorkManagerTestInitHelper.initializeTestWorkManager(
            this,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )
        super.onCreate()
    }

    companion object {
        const val TEST_EMULATOR_HOST = "127.0.0.1"
    }
}
