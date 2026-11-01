package com.repp.aquaticremineztools.domain.compressor.engine

import android.content.Context
import com.repp.aquaticremineztools.data.compressor.CloudConsentState
import com.repp.aquaticremineztools.data.compressor.CompressorPreferences
import com.repp.aquaticremineztools.data.compressor.ProviderStatus

object CompressionProviderRegistry {

    private val localProvider = NativeLocalCompressionProvider()
    private val cloudProvider = CloudProxyCompressionProvider()

    var isCloudEnabled: Boolean = false

    fun getActiveProvider(context: Context): CompressionProvider {
        val consent = CompressorPreferences.getCloudConsent(context)
        return if (isCloudEnabled && consent == CloudConsentState.GRANTED) {
            cloudProvider
        } else {
            localProvider
        }
    }

    fun getProviderStatus(context: Context, provider: CompressionProvider): ProviderStatus {
        if (!provider.isCloud) {
            return ProviderStatus.AVAILABLE
        }
        val consent = CompressorPreferences.getCloudConsent(context)
        if (consent != CloudConsentState.GRANTED) {
            return ProviderStatus.BLOCKED_PRIVACY
        }
        return ProviderStatus.LIMITED
    }
}
