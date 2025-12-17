package com.dexheimer.treeinspectorandroid.presentation.vistoria.form

import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.MultiPhotoRenderer
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.PhotoRenderer
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.RadioGroupRenderer
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.SpinnerRenderer
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.SwitchCheckboxRenderer
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.TextFieldRenderer
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.HeaderRenderer
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.SeparatorRenderer
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.CheckboxGroupRenderer
import com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.DatePickerRenderer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityComponent::class) // Escopo de Activity para View/Presenter
object VistoriaFormModule {

	@Provides
	@IntoSet // Adiciona esta implementação ao Set<FormFieldRenderer>
	fun provideTextFieldRenderer(): FormFieldRenderer = TextFieldRenderer()

	@Provides
	@IntoSet
	fun provideRadioGroupRenderer(): FormFieldRenderer = RadioGroupRenderer()

	@Provides
	@IntoSet
	fun provideSpinnerRenderer(): FormFieldRenderer = SpinnerRenderer()

	@Provides
	@IntoSet
	fun provideSwitchCheckboxRenderer(): FormFieldRenderer = SwitchCheckboxRenderer()

	@Provides
	@IntoSet
	fun providePhotoRenderer(): FormFieldRenderer = PhotoRenderer()

	@Provides
	@IntoSet
	fun provideMultiPhotoRenderer(): FormFieldRenderer = MultiPhotoRenderer()

    @Provides
    @IntoSet
    fun provideHeaderRenderer(): FormFieldRenderer = HeaderRenderer()

    @Provides
    @IntoSet
    fun provideSeparatorRenderer(): FormFieldRenderer = SeparatorRenderer()

    @Provides
    @IntoSet
    fun provideCheckboxGroupRenderer(): FormFieldRenderer = CheckboxGroupRenderer()

    @Provides
    @IntoSet
    fun provideDatePickerRenderer(): FormFieldRenderer = DatePickerRenderer()

    @Provides
    @IntoSet
    fun provideTreeSpeciesRenderer(apiService: com.dexheimer.treeinspectorandroid.data.remote.ApiService): FormFieldRenderer = 
        com.dexheimer.treeinspectorandroid.presentation.vistoria.form.renderers.TreeSpeciesRenderer(apiService)
}