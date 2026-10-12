package com.repp.aquaticremineztools.presentation.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.repp.aquaticremineztools.R

class OnboardingAdapter(
    private val slides: List<OnboardingSlide>,
) : RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder>() {

    private val TYPE_NORMAL = 0
    private val TYPE_SPECIAL = 1

    override fun getItemViewType(position: Int): Int {
        return if (position == slides.size - 1) TYPE_SPECIAL else TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val layoutRes = if (viewType == TYPE_SPECIAL) {
            R.layout.item_onboarding_special
        } else {
            R.layout.item_onboarding
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        holder.prepareForReuse()
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size

    fun hideSlideContent(viewPager: ViewPager2, position: Int) {
        runOnHolder(viewPager, position) { it.resetToInvisible() }
    }

    fun showSlideInstant(viewPager: ViewPager2, position: Int) {
        runOnHolder(viewPager, position) { it.makeVisibleInstant() }
    }

    fun animateCascade(viewPager: ViewPager2, position: Int) {
        runOnHolder(viewPager, position) { it.animateCascade() }
    }

    fun clearAllSlideStates(viewPager: ViewPager2) {
        val recyclerView = viewPager.getChildAt(0) as? RecyclerView ?: return
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val holder = recyclerView.getChildViewHolder(child) as? SlideViewHolder
            holder?.prepareForReuse()
            holder?.resetToInvisible()
        }
    }

    private fun runOnHolder(
        viewPager: ViewPager2,
        position: Int,
        action: (SlideViewHolder) -> Unit,
    ) {
        tryRun(viewPager, position, action, retryCount = 0)
    }

    private fun tryRun(
        viewPager: ViewPager2,
        position: Int,
        action: (SlideViewHolder) -> Unit,
        retryCount: Int,
    ) {
        viewPager.post {
            val recyclerView = viewPager.getChildAt(0) as? RecyclerView
            val holder = recyclerView?.findViewHolderForAdapterPosition(position) as? SlideViewHolder
            if (holder != null) {
                action(holder)
            } else if (retryCount < 10) {
                viewPager.postDelayed({
                    tryRun(viewPager, position, action, retryCount + 1)
                }, 50L)
            }
        }
    }

    class SlideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivLogo: ImageView? = itemView.findViewById(R.id.ivLogo)
        private val tvLabel: TextView? = itemView.findViewById(R.id.tvLabel)
        private val tvJapanese: TextView? = itemView.findViewById(R.id.tvJapanese)
        private val tvTitle: TextView? = itemView.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView? = itemView.findViewById(R.id.tvSubtitle)
        private val tvDescription: TextView? = itemView.findViewById(R.id.tvDescription)
        private val tvFooterStatus: TextView? = itemView.findViewById(R.id.tvFooterStatus)

        private val viewsToAnimate: List<View> by lazy {
            listOfNotNull(
                ivLogo, tvLabel, tvJapanese, tvTitle, tvSubtitle, tvDescription,
                tvFooterStatus,
            )
        }

        /**
         * Cancel SEMUA animasi + reset ke state default.
         * Khusus logo, reset lebih detail biar ga nyangkut.
         */
        fun prepareForReuse() {
            viewsToAnimate.forEach { view ->
                view.animate().cancel()
                view.clearAnimation()
                view.alpha = 1f
                view.translationY = 0f
                view.translationX = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.rotation = 0f
                view.visibility = View.VISIBLE
            }

            // Khusus logo: reset ekstra
            ivLogo?.let { logo ->
                logo.animate().cancel()
                logo.clearAnimation()
                logo.alpha = 1f
                logo.scaleX = 1f
                logo.scaleY = 1f
                logo.translationY = 0f
                logo.translationX = 0f
            }
        }

        fun bind(slide: OnboardingSlide) {
            val cyberBlastTypeface = try {
                ResourcesCompat.getFont(itemView.context, R.font.cyber_blast)
            } catch (_: Exception) {
                null
            }
            if (cyberBlastTypeface != null) {
                tvTitle?.typeface = cyberBlastTypeface
                tvSubtitle?.typeface = cyberBlastTypeface
            }

            ivLogo?.setImageResource(R.drawable.logo_app)
            tvLabel?.setText(slide.labelRes)
            tvJapanese?.setText(slide.japaneseRes)
            tvTitle?.setText(slide.titleRes)
            tvSubtitle?.setText(slide.subtitleRes)
            tvDescription?.setText(slide.descriptionRes)

            resetToInvisible()
        }

        /**
         * Set INVISIBLE — termasuk logo.
         */
        fun resetToInvisible() {
            viewsToAnimate.forEach { view ->
                view.animate().cancel()
                view.clearAnimation()
                view.alpha = 0f
                view.translationY = 60f
                view.translationX = 0f
                view.scaleX = 0.85f
                view.scaleY = 0.85f
                view.rotation = 0f
            }

            // Logo WAJIB invisible
            ivLogo?.let {
                it.animate().cancel()
                it.clearAnimation()
                it.alpha = 0f
                it.scaleX = 0.85f
                it.scaleY = 0.85f
                it.translationY = 60f
                it.translationX = 0f
            }
        }

        /**
         * Set VISIBLE instant — termasuk logo.
         */
        fun makeVisibleInstant() {
            viewsToAnimate.forEach { view ->
                view.animate().cancel()
                view.clearAnimation()
                view.alpha = 1f
                view.translationY = 0f
                view.translationX = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.rotation = 0f
            }

            // Logo WAJIB visible instant
            ivLogo?.let {
                it.animate().cancel()
                it.clearAnimation()
                it.alpha = 1f
                it.scaleX = 1f
                it.scaleY = 1f
                it.translationY = 0f
                it.translationX = 0f
            }
        }

        fun animateCascade() {
            viewsToAnimate.forEach { view ->
                view.animate().cancel()
                view.clearAnimation()
                view.alpha = 0f
                view.translationY = 60f
                view.translationX = 0f
                view.scaleX = 0.85f
                view.scaleY = 0.85f
                view.rotation = 0f
            }

            viewsToAnimate.forEachIndexed { index, view ->
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .setStartDelay(index * 100L)
                    .setInterpolator(OvershootInterpolator(1.1f))
                    .start()
            }
        }
    }
}