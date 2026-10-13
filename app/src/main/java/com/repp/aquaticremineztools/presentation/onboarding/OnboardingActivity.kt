package com.repp.aquaticremineztools.presentation.onboarding

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.viewpager2.widget.ViewPager2
import com.repp.aquaticremineztools.BuildConfig
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.presentation.home.HomeActivity
import kotlin.math.abs

class OnboardingActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var videoBackground: PlayerView
    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: AppCompatButton
    private lateinit var btnBack: AppCompatButton
    private lateinit var btnSkip: TextView
    private lateinit var btnMute: FrameLayout
    private lateinit var ivMuteIcon: ImageView
    private lateinit var dotIndicator: LinearLayout
    private lateinit var adapter: OnboardingAdapter

    private val totalSlides = 4
    private val dots = mutableListOf<View>()

    private enum class TransitionMode { NEXT, BACK }
    private var transitionMode = TransitionMode.NEXT

    private var lastHandledPosition = -1
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            getSharedPreferences("app_prefs", MODE_PRIVATE).edit {
                putBoolean("onboarding_done", false)
            }
        }

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_done", false)) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_onboarding)

        initViews()
        setupVideo()
        setupViewPager()
        setupDots()
        setupButtons()
        setupMuteButton()

        updateSlideUI(0)
    }

    private fun initViews() {
        videoBackground = findViewById(R.id.videoBackground)
        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        btnSkip = findViewById(R.id.btnSkip)
        btnMute = findViewById(R.id.btnMute)
        ivMuteIcon = findViewById(R.id.ivMuteIcon)
        dotIndicator = findViewById(R.id.dotIndicator)
    }

    private fun setupVideo() {
        player = ExoPlayer.Builder(this).build()
        val videoUri = "android.resource://$packageName/${R.raw.onboarding_video}".toUri()

        player.setMediaItem(MediaItem.fromUri(videoUri))
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.volume = 1.0f
        player.playWhenReady = true
        player.prepare()

        videoBackground.player = player
    }

    private fun setupMuteButton() {
        btnMute.setOnClickListener {
            isMuted = !isMuted
            player.volume = if (isMuted) 0f else 1.0f
            ivMuteIcon.setImageResource(
                if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on,
            )
        }
    }

    private fun setupViewPager() {
        val slides = listOf(
            OnboardingSlide(
                labelRes = R.string.onboarding_slide1_label,
                japaneseRes = R.string.onboarding_slide1_japanese,
                titleRes = R.string.onboarding_slide1_title,
                subtitleRes = R.string.onboarding_slide1_subtitle,
                descriptionRes = R.string.onboarding_slide1_desc,
            ),
            OnboardingSlide(
                labelRes = R.string.onboarding_slide2_label,
                japaneseRes = R.string.onboarding_slide2_japanese,
                titleRes = R.string.onboarding_slide2_title,
                subtitleRes = R.string.onboarding_slide2_subtitle,
                descriptionRes = R.string.onboarding_slide2_desc,
            ),
            OnboardingSlide(
                labelRes = R.string.onboarding_slide3_label,
                japaneseRes = R.string.onboarding_slide3_japanese,
                titleRes = R.string.onboarding_slide3_title,
                subtitleRes = R.string.onboarding_slide3_subtitle,
                descriptionRes = R.string.onboarding_slide3_desc,
            ),
            OnboardingSlide(
                labelRes = R.string.onboarding_slide4_label,
                japaneseRes = R.string.onboarding_slide4_japanese,
                titleRes = R.string.onboarding_slide4_title,
                subtitleRes = R.string.onboarding_slide4_subtitle,
                descriptionRes = R.string.onboarding_slide4_desc,
            ),
        )

        adapter = OnboardingAdapter(slides)
        viewPager.adapter = adapter

        viewPager.setPageTransformer { page, position ->
            val absPos = abs(position)
            page.translationX = -position * page.width * 0.2f
            val scale = 1f - (absPos * 0.1f)
            page.scaleX = scale
            page.scaleY = scale
        }

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    if (state != ViewPager2.SCROLL_STATE_IDLE) return

                    val currentPos = viewPager.currentItem
                    if (currentPos == lastHandledPosition) {
                        transitionMode = TransitionMode.NEXT
                        return
                    }

                    when (transitionMode) {
                        TransitionMode.NEXT -> {
                            adapter.hideSlideContent(viewPager, currentPos)
                            adapter.animateCascade(viewPager, currentPos)
                        }
                        TransitionMode.BACK -> {
                            adapter.showSlideInstant(viewPager, currentPos)
                        }
                    }

                    transitionMode = TransitionMode.NEXT
                    lastHandledPosition = currentPos
                }

                override fun onPageSelected(position: Int) {
                    updateDots(position)
                }
            },
        )

        viewPager.postDelayed({
            lastHandledPosition = 0
            adapter.animateCascade(viewPager, 0)
        }, 300)
    }

    private fun updateSlideUI(position: Int) {
        btnBack.visibility = if (position > 0) View.VISIBLE else View.GONE
        btnSkip.visibility = View.VISIBLE

        if (position == totalSlides - 1) {
            btnNext.text = getString(R.string.onboarding_start)
        } else {
            btnNext.text = getString(R.string.onboarding_next)
        }
    }

    private fun setupDots() {
        val density = resources.displayMetrics.density
        val activeWidth = (28 * density).toInt()
        val inactiveWidth = (8 * density).toInt()
        val height = (8 * density).toInt()

        dotIndicator.removeAllViews()
        dots.clear()

        for (i in 0 until totalSlides) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(
                if (i == 0) activeWidth else inactiveWidth,
                height,
            )
            params.marginEnd = (8 * density).toInt()
            dot.layoutParams = params

            dot.setBackgroundResource(
                if (i == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive,
            )

            dots.add(dot)
            dotIndicator.addView(dot)
        }
    }

    private fun updateDots(position: Int) {
        val density = resources.displayMetrics.density
        val activeWidth = (28 * density).toInt()
        val inactiveWidth = (8 * density).toInt()
        val height = (8 * density).toInt()

        dots.forEachIndexed { index, dot ->
            val isActive = index == position
            val targetWidth = if (isActive) activeWidth else inactiveWidth
            val currentWidth = dot.layoutParams.width

            if (currentWidth != targetWidth) {
                val widthAnim = ValueAnimator.ofInt(currentWidth, targetWidth)
                widthAnim.duration = 300
                widthAnim.interpolator = AccelerateDecelerateInterpolator()
                widthAnim.addUpdateListener { anim ->
                    val value = anim.animatedValue as Int
                    val params = dot.layoutParams
                    params.width = value
                    params.height = height
                    dot.layoutParams = params
                }
                widthAnim.start()
            }

            dot.setBackgroundResource(
                if (isActive) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive,
            )
        }
    }

    private fun setupButtons() {
        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < totalSlides - 1) {
                transitionMode = TransitionMode.NEXT
                updateSlideUI(current + 1)
                adapter.clearAllSlideStates(viewPager)
                adapter.hideSlideContent(viewPager, current + 1)
                viewPager.setCurrentItemSmooth(current + 1, 500L)
            } else {
                finishOnboarding()
            }
        }

        btnBack.setOnClickListener {
            val current = viewPager.currentItem
            if (current > 0) {
                transitionMode = TransitionMode.BACK
                updateSlideUI(current - 1)
                adapter.clearAllSlideStates(viewPager)
                adapter.showSlideInstant(viewPager, current - 1)
                viewPager.setCurrentItemSmooth(current - 1, 500L)
            }
        }

        btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun finishOnboarding() {
        fadeOutPlayer()
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit {
            putBoolean("onboarding_done", true)
        }
        btnNext.postDelayed({ goToMain() }, 400)
    }

    private fun fadeOutPlayer() {
        if (!::player.isInitialized) return
        val duration = 400L
        val steps = 20
        val interval = duration / steps
        val volumeStep = player.volume / steps

        btnNext.postDelayed(
            object : Runnable {
                var count = 0
                override fun run() {
                    if (count < steps && ::player.isInitialized) {
                        player.volume = (player.volume - volumeStep).coerceAtLeast(0f)
                        count++
                        btnNext.postDelayed(this, interval)
                    } else if (::player.isInitialized) {
                        player.volume = 0f
                    }
                }
            },
            interval,
        )
    }

    private fun goToMain() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    override fun onPause() {
        super.onPause()
        if (::player.isInitialized) player.pause()
    }

    override fun onResume() {
        super.onResume()
        if (::player.isInitialized) player.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) player.release()
    }
}

private fun ViewPager2.setCurrentItemSmooth(
    item: Int,
    duration: Long = 500L,
    interpolator: TimeInterpolator = AccelerateDecelerateInterpolator(),
) {
    if (isFakeDragging) return
    val pxToScroll = (item - currentItem) * width
    val animator = ValueAnimator.ofInt(0, pxToScroll)
    var previousValue = 0

    animator.addUpdateListener { valueAnimator ->
        val currentValue = valueAnimator.animatedValue as Int
        val currentPxToScroll = (currentValue - previousValue).toFloat()
        fakeDragBy(-currentPxToScroll)
        previousValue = currentValue
    }

    animator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
            beginFakeDrag()
        }

        override fun onAnimationEnd(animation: Animator) {
            if (isFakeDragging) endFakeDrag()
        }

        override fun onAnimationCancel(animation: Animator) {
            if (isFakeDragging) endFakeDrag()
        }
    })

    animator.interpolator = interpolator
    animator.duration = duration
    animator.start()
}