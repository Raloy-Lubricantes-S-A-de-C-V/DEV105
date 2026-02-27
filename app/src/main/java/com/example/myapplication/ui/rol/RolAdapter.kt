package com.example.myapplication.ui.rol

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.network.RolData

class RolAdapter(
    private var list: List<RolData>,
    private val onSelected: (RolData) -> Unit
) : RecyclerView.Adapter<RolAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtUser: TextView = view.findViewById(R.id.txtTableUser)
        val txtRoles: TextView = view.findViewById(R.id.txtTableRoles)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rol_table, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtUser.text = item.user
        holder.txtRoles.text = "S:${item.sys} A:${item.admin} N:${item.normal}"

        holder.itemView.setOnClickListener { onSelected(item) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<RolData>) {
        this.list = newList
        notifyDataSetChanged()
    }
}