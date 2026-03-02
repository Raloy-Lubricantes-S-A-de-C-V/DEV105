package com.example.myapplication.ui.checkin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.network.CheckInData

class CheckInAdapter(private var list: List<CheckInData>) : RecyclerView.Adapter<CheckInAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAlbaran: TextView = view.findViewById(R.id.tvAlbaranName)
        val tvStatus: TextView = view.findViewById(R.id.tvConfirmStatus)
        val tvSignatureDate: TextView = view.findViewById(R.id.tvSignatureDate) // 🔥 Añadido
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checkin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvAlbaran.text = item.albaran ?: "Sin Albarán"

        if (item.confirm == 1) {
            holder.tvStatus.text = "CONFIRMADO"
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"))

            // 🔥 Mostramos la fecha de confirmación devuelta por el servidor
            if (!item.signature_date.isNullOrEmpty()) {
                holder.tvSignatureDate.text = "Actualizado: ${item.signature_date}"
                holder.tvSignatureDate.visibility = View.VISIBLE
            } else {
                holder.tvSignatureDate.visibility = View.GONE
            }
        } else {
            holder.tvStatus.text = "PENDIENTE"
            holder.tvStatus.setTextColor(Color.parseColor("#F44336"))
            holder.tvSignatureDate.visibility = View.GONE
        }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<CheckInData>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = list[oldItemPosition].id == newList[newItemPosition].id
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = list[oldItemPosition] == newList[newItemPosition]
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.list = newList
        diffResult.dispatchUpdatesTo(this)
    }
}