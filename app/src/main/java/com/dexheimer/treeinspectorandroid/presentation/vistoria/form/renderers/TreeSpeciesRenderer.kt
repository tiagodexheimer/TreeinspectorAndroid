
package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class TreeSpeciesRenderer @Inject constructor(
    private val apiService: ApiService
) : FormFieldRenderer {

    override val supportedTypes: List<String> = listOf("tree_species")

    override fun render(context: Context, field: FormField, container: ViewGroup): View {
        val textInputLayout = TextInputLayout(context)
        textInputLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        textInputLayout.hint = field.label
        textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        
        // Fix: Ensure minimum height for touch target
        textInputLayout.minimumHeight = (context.resources.displayMetrics.density * 56).toInt()

        val autoComplete = AutoCompleteTextView(textInputLayout.context)
        autoComplete.layoutParams = LinearLayout.LayoutParams(
             LinearLayout.LayoutParams.MATCH_PARENT,
             LinearLayout.LayoutParams.WRAP_CONTENT
        )
        autoComplete.inputType = android.text.InputType.TYPE_CLASS_TEXT
        autoComplete.threshold = 1 // Start searching after 1 char
        
        // Fix: Add padding and min height usage
        val padding = (context.resources.displayMetrics.density * 16).toInt()
        autoComplete.setPadding(padding, padding, padding, padding)
        autoComplete.minHeight = (context.resources.displayMetrics.density * 48).toInt()
        
        // Initial empty adapter
        var currentAdapter = ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line)
        autoComplete.setAdapter(currentAdapter)

        var searchJob: Job? = null

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                
                val query = s?.toString() ?: ""
                if (query.length < 2) return

                val lifecycleOwner = autoComplete.findViewTreeLifecycleOwner()
                
                lifecycleOwner?.lifecycleScope?.launch {
                     searchJob = launch {
                         delay(400) // Reduced debounce
                         try {
                             val response = apiService.getSpecies(query)
                             if (response.isSuccessful) {
                                  val speciesList = response.body()?.results ?: emptyList()
                                  val names = speciesList.map { "${it.nomeComum} (${it.nomeCientifico})" }
                                  
                                  // Fix: Create fresh adapter to avoid Filter issues with Async results
                                  val newAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, names)
                                  autoComplete.setAdapter(newAdapter)
                                  
                                  if (names.isNotEmpty() && autoComplete.hasFocus()) {
                                      autoComplete.showDropDown()
                                  }
                             }
                         } catch (e: Exception) {
                             e.printStackTrace()
                         }
                     }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        textInputLayout.addView(autoComplete)
        container.addView(textInputLayout)
        addSpacer(context, container)

        return autoComplete
    }

    override fun collectResponse(view: View, field: FormField): Any? {
        return (view as AutoCompleteTextView).text.toString()
    }

    private fun addSpacer(context: Context, container: ViewGroup) {
        val spacer = View(context)
        spacer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (context.resources.displayMetrics.density * 16).toInt()
        )
        container.addView(spacer)
    }
}
