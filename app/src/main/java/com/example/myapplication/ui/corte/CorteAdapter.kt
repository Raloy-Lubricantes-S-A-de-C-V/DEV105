package com.example.myapplication.ui.corte

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.network.CorteData
import com.example.myapplication.databinding.ItemCorteBinding

class CorteAdapter(
    private var list: List<CorteData>,
    private val onItemSelected: (CorteData) -> Unit
) : RecyclerView.Adapter<CorteAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCorteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCorteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvDescription.text = item.description
        holder.binding.tvStatus.text = if (item.state == 1) "Estado: ABIERTO" else "Estado: CERRADO"

        holder.itemView.setOnClickListener { onItemSelected(item) }
    }

    override fun getItemCount(): Int = list.size

    // ✅ Esto es vital para que la tabla se refresque al recibir datos
    fun updateData(newList: List<CorteData>) {
        this.list = newList
        notifyDataSetChanged()
    }
}