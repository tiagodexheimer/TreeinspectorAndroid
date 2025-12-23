package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import javax.inject.Inject

class SeparatorRenderer @Inject constructor() : FormFieldRenderer {

    override val supportedTypes = listOf("separator")

    override fun render(context: Context, field: FormField, container: ViewGroup): View {
        val separator = View(context).apply {
            setBackgroundColor(Color.parseColor("#DDDDDD")) // Cor levemente mais visível
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.density * 1).toInt() // Altura: 1dp
            ).apply {
                setMargins(0, 16, 0, 8) 
            }
        }
        container.addView(separator)
        addSpacer(context, container)
        return separator
    }

    override fun collectResponse(view: View, field: FormField): Any? {
        return null // Separadores não têm resposta
    }

    private fun addSpacer(context: Context, container: ViewGroup) {
        val spacer = View(context)
        spacer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (context.resources.displayMetrics.density * 8).toInt()
        )
        container.addView(spacer)
    }
}
