package com.repp.aquaticremineztools.presentation.onboarding

import androidx.annotation.StringRes

data class OnboardingSlide(
    @get:StringRes val labelRes: Int,
    @get:StringRes val japaneseRes: Int,
    @get:StringRes val titleRes: Int,
    @get:StringRes val subtitleRes: Int,
    @get:StringRes val descriptionRes: Int,
)