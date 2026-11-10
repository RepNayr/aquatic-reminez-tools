package com.repp.aquaticremineztools.data.manga.source

sealed class SessionValidationResult {
    object Valid : SessionValidationResult()
    data class Unauthenticated(val reason: String) : SessionValidationResult()
    data class AccessDenied(val code: Int) : SessionValidationResult()
    object ChallengeRequired : SessionValidationResult()
    data class NetworkError(val error: Throwable) : SessionValidationResult()
}
