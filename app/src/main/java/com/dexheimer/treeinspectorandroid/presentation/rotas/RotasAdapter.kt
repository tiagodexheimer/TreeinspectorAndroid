package com.dexheimer.treeinspectorandroid.presentation.rotas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.domain.model.Rota

// Remova a importação do Chip se não for mais usada
// import com.google.android.material.chip.Chip

// O adapter recebe a lista de rotas (do seu Rota.kt) e a função de clique
class RotaAdapter(
	private var rotas: List<Rota>,
	private val onItemClick: (Int) -> Unit // Função lambda que recebe o ID da rota
) : RecyclerView.Adapter<RotaAdapter.RotaViewHolder>() {

	// 1. ViewHolder: Mapeia as Views do NOVO item_rota.xml
	class RotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val nomeTextView: TextView = itemView.findViewById(R.id.rotaNomeTextView)
		val demandasTextView: TextView = itemView.findViewById(R.id.rotaDemandasTextView)
		val dataTextView: TextView = itemView.findViewById(R.id.rotaDataTextView)
		// Os campos de responsável e status foram removidos
	}

	// 2. onCreateViewHolder: Infla o layout (sem mudança aqui)
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RotaViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_rota, parent, false)
		return RotaViewHolder(view)
	}

	// 3. getItemCount: (Sem mudança aqui)
	override fun getItemCount() = rotas.size

	// 4. onBindViewHolder: Conecta os dados do SEU Rota.kt com as Views
	override fun onBindViewHolder(holder: RotaViewHolder, position: Int) {
		val rota = rotas[position]

		holder.nomeTextView.text = rota.nome

		// CORREÇÃO: total_demandas -> totalDemandas
		holder.demandasTextView.text = "Demandas: ${rota.totalDemandas}"

		// CORREÇÃO: created_at -> dataCriacao
		// Nota: No Domain definimos como dataCriacao
		val dataFormatada = rota.dataCriacao?.split("T")?.firstOrNull() ?: rota.dataCriacao
		holder.dataTextView.text = "Criada em: $dataFormatada"

		holder.itemView.setOnClickListener {
			onItemClick(rota.id)
		}
	}

	// 5. Função para atualizar a lista (sem mudança aqui)
	fun updateData(newRotas: List<Rota>) {
		rotas = newRotas
		notifyDataSetChanged() // Informa ao adapter que os dados mudaram
	}
}