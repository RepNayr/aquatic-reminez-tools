package com.repp.aquaticremineztools.presentation.security

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R

class KeypadAdapter(
    private val onNumberClick: (String) -> Unit,
    private val onBackspaceClick: () -> Unit,
    private val onClearAllClick: () -> Unit,
) : RecyclerView.Adapter<KeypadAdapter.KeypadViewHolder>() {

    private val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "⌫", "0", "Del",
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeypadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_keypad_button, parent, false)
        return KeypadViewHolder(view)
    }

    override fun onBindViewHolder(holder: KeypadViewHolder, position: Int) {
        val key = keys[position]
        val context = holder.itemView.context

        when (key) {
            "⌫" -> {
                // Backspace — icon panah kiri
                holder.tvNumber.text = ""
                holder.tvNumber.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_backspace, 0, 0, 0
                )
                holder.tvNumber.compoundDrawablePadding = 0
                holder.tvNumber.textSize = 28f
                holder.tvNumber.setTextColor(context.getColor(R.color.text_primary))
                holder.root.setOnClickListener { onBackspaceClick() }
            }
            "Del" -> {
                // Clear All — teks "Del"
                holder.tvNumber.text = "Del"
                holder.tvNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                holder.tvNumber.compoundDrawablePadding = 0
                holder.tvNumber.textSize = 18f
                holder.tvNumber.setTextColor(context.getColor(R.color.accent_red))
                holder.root.setOnClickListener { onClearAllClick() }
            }
            else -> {
                // Angka biasa
                holder.tvNumber.text = key
                holder.tvNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                holder.tvNumber.compoundDrawablePadding = 0
                holder.tvNumber.textSize = 28f
                holder.tvNumber.setTextColor(context.getColor(R.color.text_primary))
                holder.root.setOnClickListener { onNumberClick(key) }
            }
        }
    }

    override fun getItemCount(): Int = keys.size

    class KeypadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val root: FrameLayout = itemView.findViewById(R.id.keypadRoot)
        val tvNumber: TextView = itemView.findViewById(R.id.tvKeypadNumber)
    }
}