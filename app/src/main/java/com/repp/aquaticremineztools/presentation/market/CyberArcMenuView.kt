package com.repp.aquaticremineztools.presentation.market

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.repp.aquaticremineztools.R
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class CyberArcState {
    CLOSED,
    OPENING,
    OPEN,
    CLOSING,
    NAVIGATING
}

data class CyberArcDestination(
    val destination: MarketNavDestination,
    val label: String,
    val iconResId: Int,
    val polarRadiusDp: Float,
    val polarAngleDeg: Float
)

class CyberArcMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val destinations = listOf(
        CyberArcDestination(MarketNavDestination.OVERVIEW, "Overview", R.drawable.ic_tab_overview, 120f, 115f),
        CyberArcDestination(MarketNavDestination.MARKETS, "Markets", R.drawable.ic_categories, 165f, 135f),
        CyberArcDestination(MarketNavDestination.WATCHLIST, "Watchlist", R.drawable.ic_tab_watchlist, 210f, 155f),
        CyberArcDestination(MarketNavDestination.PORTFOLIO, "Portfolio", R.drawable.ic_tab_portfolio, 255f, 175f)
    )

    var currentState: CyberArcState = CyberArcState.CLOSED
        private set

    var onDestinationSelectedListener: ((MarketNavDestination) -> Unit)? = null
    var onMenuClosedListener: (() -> Unit)? = null

    private var triggerView: View? = null
    private var triggerImageView: ImageView? = null
    private var anchorX: Float = 0f
    private var anchorY: Float = 0f

    private val pillViews = mutableListOf<View>()
    private val pillIconViews = mutableListOf<ImageView>()
    private val pillLabelViews = mutableListOf<TextView>()

    private var activeTargetIndex: Int = -1
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val handler = Handler(Looper.getMainLooper())

    private var downX = 0f
    private var downY = 0f
    private var isDragging = false
    private var longPressRunnable: Runnable? = null

    private var activeAnimators: AnimatorSet? = null

    init {
        visibility = GONE
        isClickable = true
        isFocusable = true
        setupPillViews()
        setupInsetsListener()

        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (currentState == CyberArcState.OPEN || currentState == CyberArcState.OPENING) {
                updateAnchorPosition()
                layoutPills()
            }
        }
    }

    private fun setupPillViews() {
        val density = resources.displayMetrics.density
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        destinations.forEachIndexed { index, item ->
            val pillLayout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(
                    (12 * density).toInt(),
                    (8 * density).toInt(),
                    (14 * density).toInt(),
                    (8 * density).toInt()
                )
                setBackgroundResource(R.drawable.bg_cyber_arc_pill)
                elevation = 8 * density
                isClickable = true
                isFocusable = true
                contentDescription = "${item.label} navigation destination"
            }

            val iconView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams((20 * density).toInt(), (20 * density).toInt())
                setImageResource(item.iconResId)
                val iconColor = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#333333")
                setColorFilter(iconColor)
            }

            val labelView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = (6 * density).toInt()
                }
                text = item.label.uppercase()
                textSize = 11f
                letterSpacing = 0.08f
                val textColor = if (isDarkMode) Color.parseColor("#FFFFFF") else Color.parseColor("#1A1F26")
                setTextColor(textColor)
                visibility = GONE
                alpha = 0f
            }

            pillLayout.addView(iconView)
            pillLayout.addView(labelView)

            pillLayout.setOnClickListener {
                if (currentState == CyberArcState.OPEN) {
                    selectDestination(item.destination)
                }
            }

            pillLayout.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
                    if (currentState == CyberArcState.OPEN) {
                        selectDestination(item.destination)
                        true
                    } else false
                } else false
            }

            addView(pillLayout, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

            pillViews.add(pillLayout)
            pillIconViews.add(iconView)
            pillLabelViews.add(labelView)
        }
    }

    private fun setupInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            if (currentState == CyberArcState.OPEN || currentState == CyberArcState.OPENING) {
                updateAnchorPosition()
                layoutPills()
            }
            insets
        }
    }

    fun attachTrigger(trigger: View, triggerIconView: ImageView? = null) {
        this.triggerView = trigger
        this.triggerImageView = triggerIconView

        trigger.setOnTouchListener { _, event ->
            handleTriggerTouch(event)
        }
    }

    private fun updateAnchorPosition() {
        val trigger = triggerView ?: return
        val location = IntArray(2)
        trigger.getLocationOnScreen(location)

        val myLocation = IntArray(2)
        getLocationOnScreen(myLocation)

        anchorX = (location[0] - myLocation[0] + trigger.width / 2f)
        anchorY = (location[1] - myLocation[1] + trigger.height / 2f)
    }

    private fun layoutPills() {
        if (width <= 0 || height <= 0) return
        val density = resources.displayMetrics.density

        val maxRadiusDp = 255f
        val availableWidthDp = (width - 48 * density) / density
        val scaleFactor = (availableWidthDp / maxRadiusDp).coerceIn(0.65f, 1.0f)

        destinations.forEachIndexed { index, item ->
            val pill = pillViews[index]
            val rad = Math.toRadians(item.polarAngleDeg.toDouble())
            val r = item.polarRadiusDp * density * scaleFactor

            val dx = (r * cos(rad)).toFloat()
            val dy = (r * sin(rad)).toFloat()

            val centerX = anchorX + dx
            val centerY = anchorY + dy

            pill.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
            )

            val left = (centerX - pill.measuredWidth / 2f).toInt()
            val top = (centerY - pill.measuredHeight / 2f).toInt()

            pill.x = left.toFloat()
            pill.y = top.toFloat()
        }
    }

    fun openMenu() {
        if (currentState != CyberArcState.CLOSED) return
        currentState = CyberArcState.OPENING
        visibility = VISIBLE

        updateAnchorPosition()
        layoutPills()

        val isReducedMotion = isReducedMotionEnabled()

        activeAnimators?.cancel()
        val animSet = AnimatorSet()
        val animators = mutableListOf<Animator>()

        triggerImageView?.let { img ->
            img.animate().rotation(90f).setDuration(if (isReducedMotion) 0 else 180).start()
            img.setImageResource(R.drawable.ic_close)
        }

        destinations.forEachIndexed { index, item ->
            val pill = pillViews[index]
            val targetX = pill.x
            val targetY = pill.y

            if (isReducedMotion) {
                pill.x = targetX
                pill.y = targetY
                pill.alpha = 1f
                pill.scaleX = 1f
                pill.scaleY = 1f
            } else {
                pill.x = anchorX - pill.width / 2f
                pill.y = anchorY - pill.height / 2f
                pill.alpha = 0f
                pill.scaleX = 0.4f
                pill.scaleY = 0.4f

                val moveX = ObjectAnimator.ofFloat(pill, View.X, anchorX - pill.width / 2f, targetX)
                val moveY = ObjectAnimator.ofFloat(pill, View.Y, anchorY - pill.height / 2f, targetY)
                val fadeIn = ObjectAnimator.ofFloat(pill, View.ALPHA, 0f, 1f)
                val scaleX = ObjectAnimator.ofFloat(pill, View.SCALE_X, 0.4f, 1f)
                val scaleY = ObjectAnimator.ofFloat(pill, View.SCALE_Y, 0.4f, 1f)

                val pillAnim = AnimatorSet().apply {
                    playTogether(moveX, moveY, fadeIn, scaleX, scaleY)
                    duration = (180 + index * 15).toLong()
                    interpolator = DecelerateInterpolator()
                    startDelay = (index * 20).toLong()
                }
                animators.add(pillAnim)
            }
        }

        if (isReducedMotion) {
            currentState = CyberArcState.OPEN
            requestFocus()
        } else {
            animSet.playTogether(animators)
            animSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentState = CyberArcState.OPEN
                    requestFocus()
                }
            })
            activeAnimators = animSet
            animSet.start()
        }
    }

    fun closeMenu(onClosed: (() -> Unit)? = null) {
        if (currentState == CyberArcState.CLOSED || currentState == CyberArcState.CLOSING) return
        currentState = CyberArcState.CLOSING

        val isReducedMotion = isReducedMotionEnabled()

        triggerImageView?.let { img ->
            img.animate().rotation(0f).setDuration(if (isReducedMotion) 0 else 140).start()
            img.setImageResource(R.drawable.ic_menu)
        }

        activeAnimators?.cancel()

        if (isReducedMotion) {
            visibility = GONE
            currentState = CyberArcState.CLOSED
            clearTargetHighlight()
            onClosed?.invoke()
            onMenuClosedListener?.invoke()
            triggerView?.requestFocus()
            return
        }

        val animSet = AnimatorSet()
        val animators = mutableListOf<Animator>()

        pillViews.forEachIndexed { _, pill ->
            val moveX = ObjectAnimator.ofFloat(pill, View.X, pill.x, anchorX - pill.width / 2f)
            val moveY = ObjectAnimator.ofFloat(pill, View.Y, pill.y, anchorY - pill.height / 2f)
            val fadeOut = ObjectAnimator.ofFloat(pill, View.ALPHA, pill.alpha, 0f)
            val scaleX = ObjectAnimator.ofFloat(pill, View.SCALE_X, pill.scaleX, 0.3f)
            val scaleY = ObjectAnimator.ofFloat(pill, View.SCALE_Y, pill.scaleY, 0.3f)

            val pillAnim = AnimatorSet().apply {
                playTogether(moveX, moveY, fadeOut, scaleX, scaleY)
                duration = 140
                interpolator = AccelerateDecelerateInterpolator()
            }
            animators.add(pillAnim)
        }

        animSet.playTogether(animators)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = GONE
                currentState = CyberArcState.CLOSED
                clearTargetHighlight()
                onClosed?.invoke()
                onMenuClosedListener?.invoke()
                triggerView?.requestFocus()
            }
        })
        activeAnimators = animSet
        animSet.start()
    }

    fun closeImmediately() {
        activeAnimators?.cancel()
        handler.removeCallbacksAndMessages(null)
        triggerImageView?.rotation = 0f
        triggerImageView?.setImageResource(R.drawable.ic_menu)
        visibility = GONE
        currentState = CyberArcState.CLOSED
        clearTargetHighlight()
    }

    fun selectDestination(destination: MarketNavDestination) {
        if (currentState != CyberArcState.OPEN && currentState != CyberArcState.OPENING) return
        currentState = CyberArcState.NAVIGATING

        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        val selectedIndex = destinations.indexOfFirst { it.destination == destination }
        if (selectedIndex >= 0) {
            highlightTarget(selectedIndex)
        }

        closeMenu {
            onDestinationSelectedListener?.invoke(destination)
        }
    }

    private fun handleTriggerTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                isDragging = false

                longPressRunnable = Runnable {
                    if (currentState == CyberArcState.CLOSED) {
                        openMenu()
                    }
                    isDragging = true
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                handler.postDelayed(longPressRunnable!!, longPressTimeout)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                val dist = sqrt(dx * dx + dy * dy)

                if (dist > touchSlop) {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                }

                if (isDragging || currentState == CyberArcState.OPEN) {
                    updateTargetSelection(event.rawX, event.rawY)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }

                if (isDragging) {
                    if (activeTargetIndex in destinations.indices) {
                        selectDestination(destinations[activeTargetIndex].destination)
                    } else {
                        closeMenu()
                    }
                    isDragging = false
                } else {
                    if (currentState == CyberArcState.CLOSED) {
                        openMenu()
                    } else if (currentState == CyberArcState.OPEN) {
                        closeMenu()
                    }
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }
                if (currentState == CyberArcState.OPEN || currentState == CyberArcState.OPENING) {
                    closeMenu()
                }
                isDragging = false
                return true
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (currentState == CyberArcState.CLOSED) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                updateTargetSelection(event.rawX, event.rawY)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (activeTargetIndex in destinations.indices) {
                    selectDestination(destinations[activeTargetIndex].destination)
                } else {
                    closeMenu()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                closeMenu()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateTargetSelection(rawX: Float, rawY: Float) {
        val density = resources.displayMetrics.density
        val acquireRadiusPx = 40f * density
        val retainRadiusPx = 56f * density

        val myLoc = IntArray(2)
        getLocationOnScreen(myLoc)
        val touchX = rawX - myLoc[0]
        val touchY = rawY - myLoc[1]

        var nearestIndex = -1
        var nearestDist = Float.MAX_VALUE

        pillViews.forEachIndexed { i, pill ->
            val cx = pill.x + pill.width / 2f
            val cy = pill.y + pill.height / 2f
            val dx = touchX - cx
            val dy = touchY - cy
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < nearestDist) {
                nearestDist = dist
                nearestIndex = i
            }
        }

        if (activeTargetIndex < 0) {
            if (nearestDist <= acquireRadiusPx) {
                setTargetIndex(nearestIndex)
            }
        } else {
            val currentPill = pillViews[activeTargetIndex]
            val curCx = currentPill.x + currentPill.width / 2f
            val curCy = currentPill.y + currentPill.height / 2f
            val curDist = sqrt((touchX - curCx) * (touchX - curCx) + (touchY - curCy) * (touchY - curCy))

            if (curDist > retainRadiusPx) {
                if (nearestDist <= acquireRadiusPx) {
                    setTargetIndex(nearestIndex)
                } else {
                    setTargetIndex(-1)
                }
            } else if (nearestIndex != activeTargetIndex && nearestDist < curDist - (16f * density) && nearestDist <= acquireRadiusPx) {
                setTargetIndex(nearestIndex)
            }
        }
    }

    private fun setTargetIndex(index: Int) {
        if (activeTargetIndex != index) {
            activeTargetIndex = index
            if (index >= 0) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                highlightTarget(index)
            } else {
                clearTargetHighlight()
            }
        }
    }

    private fun highlightTarget(targetIndex: Int) {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val cyanColor = Color.parseColor("#43D9E6")
        val normalIconColor = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#333333")

        pillViews.forEachIndexed { i, pill ->
            val icon = pillIconViews[i]
            val label = pillLabelViews[i]

            if (i == targetIndex) {
                pill.setBackgroundResource(R.drawable.bg_cyber_arc_pill_active)
                pill.scaleX = 1.08f
                pill.scaleY = 1.08f
                icon.setColorFilter(cyanColor)
                label.visibility = VISIBLE
                label.animate().alpha(1f).setDuration(120).start()
            } else {
                pill.setBackgroundResource(R.drawable.bg_cyber_arc_pill)
                pill.scaleX = 1.0f
                pill.scaleY = 1.0f
                icon.setColorFilter(normalIconColor)
                label.animate().alpha(0f).setDuration(80).withEndAction { label.visibility = GONE }.start()
            }
        }
    }

    private fun clearTargetHighlight() {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val normalIconColor = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#333333")

        pillViews.forEachIndexed { i, pill ->
            pill.setBackgroundResource(R.drawable.bg_cyber_arc_pill)
            pill.scaleX = 1.0f
            pill.scaleY = 1.0f
            pillIconViews[i].setColorFilter(normalIconColor)
            val label = pillLabelViews[i]
            label.alpha = 0f
            label.visibility = GONE
        }
        activeTargetIndex = -1
    }

    private fun isReducedMotionEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= 34) {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            try {
                val method = AccessibilityManager::class.java.getMethod("isPrefersReducedMotionEnabled")
                val result = method.invoke(am) as? Boolean
                if (result == true) return true
            } catch (_: Exception) {}
        }
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f)
        return scale == 0.0f
    }
}
