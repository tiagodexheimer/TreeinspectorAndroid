package com.dexheimer.treeinspectorandroid.presentation.rotas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dexheimer.treeinspectorandroid.R
import com.dexheimer.treeinspectorandroid.domain.model.Rota

// Mudança Principal: Herda de ListAdapter em vez de RecyclerView.Adapter
// Isso habilita o método 'submitList' e animações automáticas
class RotasAdapter(
	private val onItemClick: (Int) -> Unit
) : ListAdapter<Rota, RotasAdapter.RotaViewHolder>(RotaDiffCallback()) {

	// ViewHolder usando os SEUS IDs do item_rota.xml
	class RotaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val nomeTextView: TextView = itemView.findViewById(R.id.rotaNomeTextView)
		val demandasTextView: TextView = itemView.findViewById(R.id.rotaDemandasTextView)
		val dataTextView: TextView = itemView.findViewById(R.id.rotaDataTextView)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RotaViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_rota, parent, false)
		return RotaViewHolder(view)
	}

	override fun onBindViewHolder(holder: RotaViewHolder, position: Int) {
		// 'getItem(position)' é o método padrão do ListAdapter
		val rota = getItem(position)

		holder.nomeTextView.text = rota.nome

		// Tratamento seguro para nulos
		holder.demandasTextView.text = "Demandas: ${rota.totalDemandas ?: 0}"

		val dataFormatada = rota.dataCriacao?.split("T")?.firstOrNull() ?: rota.dataCriacao ?: "-"
		holder.dataTextView.text = "Criada em: $dataFormatada"

		holder.itemView.setOnClickListener {
			onItemClick(rota.id)
		}
	}

	// Esta classe ensina ao Android como calcular a diferença entre listas
	// para fazer a animação suave no SwipeRefresh
	class RotaDiffCallback : DiffUtil.ItemCallback<Rota>() {
		override fun areItemsTheSame(oldItem: Rota, newItem: Rota): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(oldItem: Rota, newItem: Rota): Boolean {
			return oldItem == newItem
		}
	}
}