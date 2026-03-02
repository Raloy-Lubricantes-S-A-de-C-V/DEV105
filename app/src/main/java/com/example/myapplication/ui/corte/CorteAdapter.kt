package com.example.myapplication.ui.corte

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.network.CorteData

class CorteAdapter(
    private var list: List<CorteData>,
    private val onSelected: (CorteData) -> Unit
) : RecyclerView.Adapter<CorteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDesc: TextView = view.findViewById(R.id.txtCorteDesc)
        val txtStatus: TextView = view.findViewById(R.id.txtCorteStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_corte, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtDesc.text = item.description

        // Formato visual dependiendo del estado del corte
        if (item.start_day != null) {
            holder.txtStatus.text = "INICIADO: ${item.start_day}"
        } else {
            holder.txtStatus.text = "PENDIENTE"
        }

        holder.itemView.setOnClickListener { onSelected(item) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<CorteData>) {
        this.list = newList
        notifyDataSetChanged()
    }
}