package com.example.lab_week_08.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.Worker
import androidx.work.WorkerParameters

class ThirdWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val id = inputData.getString(INPUT_DATA_ID) ?: "unknown-3"
        // simulasi kerja pendek biar gak nabrak toast
        Thread.sleep(1500L)
        val output = Data.Builder()
            .putString(OUTPUT_DATA_ID, "third-$id")
            .build()
        return Result.success(output)
    }

    companion object {
        const val INPUT_DATA_ID = "inId"
        const val OUTPUT_DATA_ID = "outId3"
    }
}
