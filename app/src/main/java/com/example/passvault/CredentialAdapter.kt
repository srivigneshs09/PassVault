package com.example.passvault

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.passvault.databinding.ItemCredentialBinding

class CredentialAdapter(
    private val list: List<Credential>,
    private val onItemClick: (Credential) -> Unit,
    private val onItemLongClick: (Credential) -> Unit
) : RecyclerView.Adapter<CredentialAdapter.VH>() {

    inner class VH(val binding: ItemCredentialBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCredentialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        holder.binding.title.text = item.title
        holder.binding.username.text = "••••••••"
        holder.binding.password.text = "••••••••"

        holder.binding.root.setOnClickListener { onItemClick(item) }
        holder.binding.root.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount() = list.size
}