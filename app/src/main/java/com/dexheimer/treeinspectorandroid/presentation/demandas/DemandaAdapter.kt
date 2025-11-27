// tiagodexheimer/treeinspectorandroid/tiagodexheimer-TreeinspectorAndroid-cf552eb3d91ad81c716ea6e58b438954df45d51d/app/src/main/java/com/dexheimer/treeinspectorandroid/presentation/demandas/DemandaAdapter.kt

package com.dexheimer.treeinspectorandroid.presentation.demandas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.domain.model.Demanda


class DemandaAdapter(
	private var demandas: List<Demanda>,
	private val onVistoriaClick: (Demanda) -> Unit
) : RecyclerView.Adapter<DemandaAdapter.DemandaViewHolder>() {

	class DemandaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val ordemTextView: TextView = itemView.findViewById(R.id.demandaOrdemTextView)
		val enderecoTextView: TextView = itemView.findViewById(R.id.demandaEnderecoTextView)
		val tipoTextView: TextView = itemView.findViewById(R.id.demandaTipoTextView)
		val vistoriaButton: Button = itemView.findViewById(R.id.btnIniciarVistoria)
		val statusTextView: TextView = itemView.findViewById(R.id.demandaStatusTextView) // <-- ADICIONADO
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemandaViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_demanda, parent, false)
		return DemandaViewHolder(view)
	}

	override fun getItemCount() = demandas.size

	override fun onBindViewHolder(holder: DemandaViewHolder, position: Int) {
		val demanda = demandas[position]

		holder.ordemTextView.text = "${position + 1}."
		// Ajuste: logradouro, numero e bairro agora são propriedades diretas da classe Demanda (Domain)
		holder.enderecoTextView.text = "${demanda.logradouro ?: "Endereço"} ${demanda.numero ?: ""} - ${demanda.bairro ?: ""}"

		// CORREÇÃO: tipo_demanda -> tipoDemanda
		holder.tipoTextView.text = "Tipo: ${demanda.tipoDemanda ?: "N/D"}"

		// CORREÇÃO: status_vistoria -> statusVistoria
		// CORREÇÃO APLICADA: Verifica se o status começa com "concluido" para incluir concluido_pendente
		if (demanda.statusVistoria.startsWith("concluido", ignoreCase = true)) {
			holder.statusTextView.text = "Concluída"
			// CORREÇÃO: Usar o recurso de cor
			holder.statusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.status_concluido))
			holder.vistoriaButton.text = "Ver / Refazer Vistoria"
		} else {
			holder.statusTextView.text = "Pendente"
			// CORREÇÃO: Usar o recurso de cor
			holder.statusTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.status_pendente))
			holder.vistoriaButton.text = "Iniciar Vistoria"
		}

		holder.vistoriaButton.setOnClickListener {
			onVistoriaClick(demanda)
		}
	}

	fun updateData(newDemandas: List<Demanda>) {
		demandas = newDemandas
		notifyDataSetChanged()
	}
}