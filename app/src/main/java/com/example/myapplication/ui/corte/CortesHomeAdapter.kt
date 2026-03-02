package com.example.myapplication.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.network.CorteData
import com.example.myapplication.databinding.ItemCorteHomeBinding

class CortesHomeAdapter(
    private val onItemSelected: (CorteData) -> Unit,
    private val onActionClick: (CorteData, Int) -> Unit
) : RecyclerView.Adapter<CortesHomeAdapter.ViewHolder>() {

    // ✅ Lista dinámica para permitir actualizaciones desde el HomeFragment
    private var list: List<CorteData> = emptyList()

    class ViewHolder(val binding: ItemCorteHomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemCorteHomeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        // Asignación de textos básicos
        holder.binding.tvDescHome.text = item.description
        holder.binding.btnIniciar.text = "Comenzar"
        holder.binding.btnParar.text = "Parar"

        // ✅ LÓGICA DE ESTADOS SEGÚN API
        // Si 'start_day' no es nulo, significa que el corte ya inició.
        val yaIniciado = item.start_day != null

        holder.binding.btnIniciar.isEnabled = !yaIniciado
        holder.binding.btnParar.isEnabled = yaIniciado

        // Manejo de clics para Iniciar (Acción 1)
        holder.binding.btnIniciar.setOnClickListener {
            holder.binding.btnIniciar.isEnabled = false
            holder.binding.btnParar.isEnabled = true
            onActionClick(item, 1)
        }

        // Manejo de clics para Parar (Acción 0)
        holder.binding.btnParar.setOnClickListener {
            holder.binding.btnParar.isEnabled = false
            holder.binding.btnIniciar.isEnabled = true
            onActionClick(item, 0)
        }

        // Selección de todo el item
        holder.itemView.setOnClickListener {
            onItemSelected(item)
        }
    }

    override fun getItemCount() = list.size

    /**
     * ✅ FUNCIÓN CLAVE: Actualiza la lista de cortes desde el HomeFragment
     * sin necesidad de recrear el adaptador.
     */
    fun updateData(newList: List<CorteData>) {
        this.list = newList
        notifyDataSetChanged()
    }
}