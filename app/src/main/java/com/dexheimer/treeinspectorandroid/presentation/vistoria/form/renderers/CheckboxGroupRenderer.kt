package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import javax.inject.Inject

class CheckboxGroupRenderer @Inject constructor() : FormFieldRenderer {

    override val supportedTypes = listOf("checkbox_group")

    override fun render(context: Context, field: FormField, container: ViewGroup, initialValue: Any?): View {
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Label do grupo
        val labelView = TextView(context).apply {
            text = field.label
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 16, 0, 8)
        }
        rootLayout.addView(labelView)

        val selectedValues = (initialValue as? String)?.split(",") ?: emptyList()

        // Opções
        field.options?.forEach { option ->
            val checkBox = CheckBox(context).apply {
                text = option.label
                tag = option.value // Armazena o valor no tag
                setTextColor(Color.BLACK)
                isChecked = selectedValues.contains(option.value)
            }
            rootLayout.addView(checkBox)
        }

        container.addView(rootLayout)
        return rootLayout
    }

    override fun collectResponse(view: View, field: FormField): Any? {
        val rootLayout = view as ViewGroup
        val selectedValues = mutableListOf<String>()

        // Começa do índice 1 porque o 0 é o Label
        for (i in 1 until rootLayout.childCount) {
            val child = rootLayout.getChildAt(i)
            if (child is CheckBox && child.isChecked) {
                selectedValues.add(child.tag.toString())
            }
        }

        return if (selectedValues.isEmpty()) null else selectedValues.joinToString(",")
    }
}
