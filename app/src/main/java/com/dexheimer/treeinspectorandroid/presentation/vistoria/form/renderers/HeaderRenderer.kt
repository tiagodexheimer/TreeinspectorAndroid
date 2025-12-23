package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import javax.inject.Inject

class HeaderRenderer @Inject constructor() : FormFieldRenderer {

    override val supportedTypes = listOf("header")

    override fun render(context: Context, field: FormField, container: ViewGroup): View {
        return TextView(context).apply {
            text = field.label
            textSize = 18f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 16) // Espaçamento top/bottom
            }
            container.addView(this)
        }
    }

    override fun collectResponse(view: View, field: FormField): Any? {
        return null // Headers não têm resposta
    }
}
