package ch.joshuah.bibleverseapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import ch.joshuah.bibleverseapp.databinding.FragmentTranslationSelectionBinding
import ch.joshuah.bibleverseapp.viewmodel.TranslationSelectionViewModel
import ch.joshuah.bibleverseapp.widgets.WidgetUpdateScheduler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class TranslationSelectionFragment : Fragment() {

    private var _binding: FragmentTranslationSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TranslationSelectionViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                return TranslationSelectionViewModel(
                    sharedPreferences = prefs,
                    defaultLanguageCode = Locale.getDefault().language
                ) as T
            }
        }
    }

    private lateinit var adapter: TranslationAdapter
    private var languageList: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslationSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TranslationAdapter { version ->
            viewModel.selectTranslation(version)
            context?.let { ctx ->
                WidgetUpdateScheduler.updateAllWidgets(ctx)
            }
            parentFragmentManager.popBackStack()
        }

        binding.rvTranslations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTranslations.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.availableLanguages.collectLatest { languages ->
                languageList = languages
                val displayNames = languages.map { langCode ->
                    val locale = Locale.forLanguageTag(langCode)
                    val name = locale.getDisplayName(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    "$name ($langCode)"
                }
                val spinnerAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    displayNames
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerLanguage.adapter = spinnerAdapter

                val currentSelected = viewModel.selectedLanguage.value
                val selectedIndex = languages.indexOf(currentSelected)
                if (selectedIndex >= 0) {
                    binding.spinnerLanguage.setSelection(selectedIndex)
                }
            }
        }

        binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in languageList.indices) {
                    val langCode = languageList[position]
                    if (langCode != viewModel.selectedLanguage.value) {
                        viewModel.selectLanguage(langCode)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredTranslations.collectLatest { translations ->
                adapter.submitList(translations, viewModel.selectedTranslationId.value)
                binding.tvEmpty.visibility = if (translations.isEmpty() && !viewModel.isLoading.value) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedTranslationId.collectLatest { selectedId ->
                adapter.updateSelectedId(selectedId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
