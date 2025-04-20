package com.example.passvault

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.passvault.databinding.ItemCredentialBinding

class CredentialAdapter(
    private val items: List<Credential>,
    private val onClick: (Credential) -> Unit
) : RecyclerView.Adapter<CredentialAdapter.VH>() {

    inner class VH(val binding: ItemCredentialBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCredentialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.titleText.text = item.title
        holder.binding.credentialInfo.text = "••••••••••"
        holder.binding.root.setOnClickListener { onClick(item) }
    }
}
