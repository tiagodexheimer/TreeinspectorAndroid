
package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dexheimer.treeinspectorandroid.data.remote.ApiService
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TreeSpeciesRenderer @Inject constructor(
    private val apiService: ApiService
) : FormFieldRenderer {

    override val supportedTypes: List<String> = listOf("tree_species")

    override fun render(context: Context, field: FormField, container: ViewGroup, initialValue: Any?): View {
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
        autoComplete.setTextColor(Color.BLACK)
        autoComplete.setHintTextColor(Color.GRAY)

        if (initialValue is String && initialValue.isNotEmpty()) {
            autoComplete.setText(initialValue)
        }
        
        // Initial empty adapter using a custom class to bypass local filtering
        class NoFilterAdapter(context: Context, resource: Int, objects: List<String>) : 
            ArrayAdapter<String>(context, resource, objects) {
            private val filter = object : android.widget.Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val results = FilterResults()
                    results.values = objects
                    results.count = objects.size
                    return results
                }
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
            override fun getFilter(): android.widget.Filter = filter
        }

        var searchJob: Job? = null

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                
                val query = s?.toString() ?: ""
                if (query.trim().isEmpty()) {
                    autoComplete.dismissDropDown()
                    return
                }

                // More robust way to find LifecycleOwner
                var currentContext = context
                var lifecycleOwner: LifecycleOwner? = autoComplete.findViewTreeLifecycleOwner()
                
                while (lifecycleOwner == null && currentContext is android.content.ContextWrapper) {
                    if (currentContext is LifecycleOwner) {
                        lifecycleOwner = currentContext
                        break
                    }
                    currentContext = currentContext.baseContext
                }
                
                if (lifecycleOwner == null) {
                    Log.e("TreeSpeciesRenderer", "Could not find LifecycleOwner for species search")
                    return
                }
                
                searchJob = lifecycleOwner.lifecycleScope.launch {
                    delay(300) 
                    try {
                        Log.d("TreeSpeciesRenderer", "Searching for: $query")
                        val response = apiService.getSpecies(query)
                        if (response.isSuccessful) {
                            val speciesList = response.body()?.results ?: emptyList()
                            val names = speciesList.map { "${it.nomeComum} (${it.nomeCientifico})" }
                            Log.d("TreeSpeciesRenderer", "Found ${names.size} results")
                            
                            withContext(Dispatchers.Main) {
                                val dropdownLayout = context.resources.getIdentifier("dropdown_item", "layout", context.packageName)
                                val newAdapter = NoFilterAdapter(context, if (dropdownLayout != 0) dropdownLayout else android.R.layout.simple_dropdown_item_1line, names)
                                autoComplete.setAdapter(newAdapter)
                                
                                if (names.isNotEmpty() && autoComplete.hasFocus()) {
                                    autoComplete.showDropDown()
                                } else {
                                    autoComplete.dismissDropDown()
                                }
                            }
                        } else {
                            Log.e("TreeSpeciesRenderer", "API Error: ${response.code()} ${response.message()}")
                        }
                    } catch (e: Exception) {
                        Log.e("TreeSpeciesRenderer", "Error fetching species", e)
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
