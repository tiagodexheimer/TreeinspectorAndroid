package com.dexheimer.treeinspectorandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DemandaAdapter(
	private var demandas: List<Demanda>,
	// Esta função será chamada quando o botão "Iniciar Vistoria" for clicado
	private val onVistoriaClick: (Demanda) -> Unit
) : RecyclerView.Adapter<DemandaAdapter.DemandaViewHolder>() {

	// Mapeia as Views do item_demanda.xml
	class DemandaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val ordemTextView: TextView = itemView.findViewById(R.id.demandaOrdemTextView)
		val enderecoTextView: TextView = itemView.findViewById(R.id.demandaEnderecoTextView)
		val tipoTextView: TextView = itemView.findViewById(R.id.demandaTipoTextView)
		val vistoriaButton: Button = itemView.findViewById(R.id.btnIniciarVistoria)
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
		holder.enderecoTextView.text = "${demanda.logradouro ?: "Endereço"} ${demanda.numero ?: ""} - ${demanda.bairro ?: ""}"
		holder.tipoTextView.text = "Tipo: ${demanda.tipo_demanda ?: "N/D"}"

		holder.vistoriaButton.setOnClickListener {
			onVistoriaClick(demanda) // Chama a função de clique passando a demanda
		}
	}

	fun updateData(newDemandas: List<Demanda>) {
		demandas = newDemandas
		notifyDataSetChanged()
	}
}