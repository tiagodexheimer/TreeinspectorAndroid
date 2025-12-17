package com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers

import android.app.DatePickerDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import com.dexheimer.treeinspectorandroid.data.remote.FormField
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.FormFieldRenderer
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class DatePickerRenderer @Inject constructor() : FormFieldRenderer {

    override val supportedTypes = listOf("date")

    override fun render(context: Context, field: FormField, container: ViewGroup): View {
        val textInputLayout = TextInputLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = field.label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        val editText = TextInputEditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isFocusable = false // Impede digitação manual
            isClickable = true
            inputType = android.text.InputType.TYPE_NULL // Impede teclado
        }

        val calendar = Calendar.getInstance()
        val updateLabel = {
            val myFormat = "yyyy-MM-dd" // Formato ISO para backend
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            editText.setText(sdf.format(calendar.time))
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateLabel()
        }

        editText.setOnClickListener {
            DatePickerDialog(context, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        textInputLayout.addView(editText)
        container.addView(textInputLayout)
        
        // Add spacer logic inline or via extension if available
        val spacer = View(context).apply {
             layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.density * 16).toInt()
            )
        }
        container.addView(spacer)

        return editText
    }

    override fun collectResponse(view: View, field: FormField): Any? {
        return (view as EditText).text.toString()
    }
}
