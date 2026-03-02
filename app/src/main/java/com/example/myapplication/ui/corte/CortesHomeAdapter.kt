package com.example.myapplication.ui.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.network.CorteData
import com.example.myapplication.databinding.ItemCorteHomeBinding
import androidx.recyclerview.widget.DiffUtil

class CortesHomeAdapter(
    private val onItemSelected: (CorteData) -> Unit,
    private val onActionClick: (CorteData, Int) -> Unit
) : RecyclerView.Adapter<CortesHomeAdapter.ViewHolder>() {

    private var list: List<CorteData> = emptyList()
    // 🔥 Control del elemento seleccionado
    private var selectedPosition = -1

    class ViewHolder(val binding: ItemCorteHomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemCorteHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.binding.tvDescHome.text = item.description
        holder.binding.btnIniciar.text = "Comenzar"
        holder.binding.btnParar.text = "Parar"

        val yaIniciado = item.start_day != null
        holder.binding.btnIniciar.isEnabled = !yaIniciado
        holder.binding.btnParar.isEnabled = yaIniciado

        // 🔥 Pintar el fondo si está seleccionado
        if (selectedPosition == position) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E0F7FA")) // Cyan claro
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.binding.btnIniciar.setOnClickListener { onActionClick(item, 1) }
        holder.binding.btnParar.setOnClickListener { onActionClick(item, 0) }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
            onItemSelected(item)
        }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<CorteData>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return list[oldItemPosition].id == newList[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return list[oldItemPosition] == newList[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.list = newList
        selectedPosition = -1 // Reseteamos la selección al recargar
        diffResult.dispatchUpdatesTo(this)
    }
}