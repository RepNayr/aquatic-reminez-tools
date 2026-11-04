package com.repp.aquaticremineztools.domain.compressor.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.repp.aquaticremineztools.data.compressor.CompressorRepository

class CancelWorkerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        try {
            val repository = CompressorRepository.getInstance(context)
            repository.cancelEntireQueue()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
