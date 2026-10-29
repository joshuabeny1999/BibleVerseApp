package ch.joshuah.bibleverseapp.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ch.joshuah.bibleverseapp.databinding.ItemTranslationBinding
import com.youversion.platform.core.bibles.models.BibleVersion

class TranslationAdapter(
    private val onTranslationSelected: (BibleVersion) -> Unit
) : RecyclerView.Adapter<TranslationAdapter.TranslationViewHolder>() {

    private var items: List<BibleVersion> = emptyList()
    private var selectedId: String? = null

    fun submitList(list: List<BibleVersion>, currentSelectedId: String? = selectedId) {
        items = list
        selectedId = currentSelectedId
        notifyDataSetChanged()
    }

    fun updateSelectedId(id: String?) {
        selectedId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranslationViewHolder {
        val binding = ItemTranslationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TranslationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TranslationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TranslationViewHolder(private val binding: ItemTranslationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(version: BibleVersion) {
            val displayTitle = version.localizedTitle?.ifBlank { version.title } ?: version.title
            val displayAbbreviation = version.localizedAbbreviation?.ifBlank { version.abbreviation } ?: version.abbreviation

            binding.tvTranslationTitle.text = displayTitle
            binding.tvTranslationAbbreviation.text = displayAbbreviation
            binding.rbSelected.isChecked = (version.id.toString() == selectedId)

            binding.root.setOnClickListener {
                onTranslationSelected(version)
            }
        }
    }
}
