package com.example.myapplication.ui.corte

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.network.CorteData

class CorteAdapter(
    private var list: List<CorteData>,
    private val onSelected: (CorteData) -> Unit
) : RecyclerView.Adapter<CorteAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "DEV105_UI"
    }

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

        if (item.start_day != null) {
            holder.txtStatus.text = "INICIADO: ${item.start_day}"
        } else {
            holder.txtStatus.text = "PENDIENTE"
        }

        holder.itemView.setOnClickListener {
            Log.d(TAG, "🖱️ Item de Corte seleccionado: ID ${item.id}")
            onSelected(item)
        }
    }

    override fun getItemCount() = list.size

    // 🔥 CAMBIO CRÍTICO: DiffUtil para calcular cambios sin saturar el GC (Garbage Collector)
    fun updateData(newList: List<CorteData>) {
        Log.d(TAG, "🔄 Calculando DiffUtil para nuevo dataset. Tamaño previo: ${list.size}, Nuevo: ${newList.size}")
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
        diffResult.dispatchUpdatesTo(this)
        Log.d(TAG, "✅ UI renderizada de manera optimizada.")
    }
}