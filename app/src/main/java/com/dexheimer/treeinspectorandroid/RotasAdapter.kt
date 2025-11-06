package com.dexheimer.treeinspectorandroid

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class RotasAdapter(context: Context, rotas: List<Rota>) :
	ArrayAdapter<Rota>(context, 0, rotas) {

	// Formatos de data para parsear o ISO (vem do banco) e formatar (para exibir)
	private val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
		timeZone = TimeZone.getTimeZone("UTC")
	}
	private val formatter = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		// 1. Pega o layout que criamos (list_item_rota.xml)
		val view = convertView ?: LayoutInflater.from(context)
			.inflate(R.layout.list_item_rota, parent, false)

		// 2. Pega o item de dados (Rota) para esta posição
		val rota = getItem(position)

		// 3. Encontra os TextViews dentro do layout
		val textViewNome = view.findViewById<TextView>(R.id.textViewRotaNome)
		val textViewData = view.findViewById<TextView>(R.id.textViewDataCriacao)
		val textViewDemandas = view.findViewById<TextView>(R.id.textViewContagemDemandas)

		if (rota != null) {
			// 4. Preenche os dados
			textViewNome.text = rota.nome

			// Formata a data (Este bloco continua igual e funciona)
			try {
				val dataFormatada = parser.parse(rota.data_criacao)?.let {
					formatter.format(it)
				} ?: "Data indisponível"
				textViewData.text = "Criada em: $dataFormatada"
			} catch (e: Exception) {
				textViewData.text = "Data inválida"
			}

			// ***** A CORREÇÃO ESTÁ AQUI *****
			// Antes: val contagem = rota.demandas.size (Dava erro)
			// Agora:
			val contagem = rota.total_demandas // Usamos o campo Int direto do JSON
			textViewDemandas.text = "$contagem ${if (contagem == 1) "demanda" else "demandas"}"
		}

		return view
	}
}