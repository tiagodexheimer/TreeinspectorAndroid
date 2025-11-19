package com.dexheimer.treeinspectorandroid.presentation.demandas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
		if (demanda.statusVistoria.equals("concluida", ignoreCase = true)) {
			holder.statusTextView.text = "Concluída"
			holder.statusTextView.setTextColor(Color.parseColor("#1A5912"))
			holder.vistoriaButton.text = "Ver / Refazer Vistoria"
		} else {
			holder.statusTextView.text = "Pendente"
			holder.statusTextView.setTextColor(Color.parseColor("#C62828"))
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