package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import javax.inject.Inject

class SeparatorRenderer @Inject constructor() : FormFieldRenderer {

    override val supportedTypes = listOf("separator")

    override fun render(context: Context, field: FormField, container: ViewGroup): View {
        return View(context).apply {
            setBackgroundColor(Color.LTGRAY) // Cor da linha
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.density * 1).toInt() // Altura: 1dp
            ).apply {
                setMargins(0, 24, 0, 24) // Margem vertical
            }
            container.addView(this)
        }
    }

    override fun collectResponse(view: View, field: FormField): Any? {
        return null // Separadores não têm resposta
    }
}
