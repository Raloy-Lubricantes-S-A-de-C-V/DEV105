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

    private var list: List<CorteData> = emptyList()
    class ViewHolder(val binding: ItemCorteHomeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemCorteHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvDescHome.text = item.description
        holder.binding.btnIniciar.text = "Comenzar"
        holder.binding.btnParar.text = "Parar"

        holder.binding.btnIniciar.isEnabled = item.start_day == null
        holder.binding.btnIniciar.setOnClickListener {
            holder.binding.btnIniciar.isEnabled = false
            onActionClick(item, 1)
        }
        holder.binding.btnParar.setOnClickListener { onActionClick(item, 0) }
        holder.itemView.setOnClickListener { onItemSelected(item) }
    }

    override fun getItemCount() = list.size
    fun updateData(newList: List<CorteData>) { list = newList; notifyDataSetChanged() }
}