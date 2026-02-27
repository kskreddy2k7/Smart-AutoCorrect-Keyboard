package com.smartautocorrect.keyboard.ui.keyboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartautocorrect.keyboard.databinding.ItemSuggestionBinding
import com.smartautocorrect.keyboard.domain.model.WordSuggestion

/**
 * RecyclerView adapter for the suggestion bar.
 * Shows up to 3 word suggestions; calls [onSuggestionClick] when user taps a suggestion.
 */
class SuggestionAdapter(
    private val onSuggestionClick: (WordSuggestion) -> Unit
) : ListAdapter<WordSuggestion, SuggestionAdapter.SuggestionViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ItemSuggestionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SuggestionViewHolder(
        private val binding: ItemSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(suggestion: WordSuggestion) {
            binding.tvSuggestion.text = suggestion.word
            // Highlight autocorrect suggestions
            binding.root.alpha = if (suggestion.isAutocorrect) 1.0f else 0.85f
            binding.root.setOnClickListener { onSuggestionClick(suggestion) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WordSuggestion>() {
            override fun areItemsTheSame(oldItem: WordSuggestion, newItem: WordSuggestion) =
                oldItem.word == newItem.word

            override fun areContentsTheSame(oldItem: WordSuggestion, newItem: WordSuggestion) =
                oldItem == newItem
        }
    }
}
