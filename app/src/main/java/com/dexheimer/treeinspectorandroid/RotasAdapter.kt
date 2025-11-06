package com.dexheimer.treeinspectorandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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

		// Preenche os dados na tela
		holder.nomeTextView.text = rota.nome
		// A linha abaixo (41) funcionará após você atualizar 'Rota.kt'
		holder.demandasTextView.text = "Demandas: ${rota.total_demandas ?: 0}"

		// ======== CORREÇÃO ESTÁ AQUI (Linha 46 do arquivo original) ========
		// Trocamos 'data_criacao' por 'created_at' e adicionamos null-safety ('?.')
		val dataFormatada = rota.created_at?.split("T")?.firstOrNull() ?: rota.created_at
		holder.dataTextView.text = "Criada em: $dataFormatada"
		// ====================================================================

		// A lógica de cor e status foi removida

		// Define a ação de clique no item (sem mudança aqui)
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