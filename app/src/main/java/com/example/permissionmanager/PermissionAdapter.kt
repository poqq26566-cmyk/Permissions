package com.example.permissionmanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.permissionmanager.databinding.ItemPermissionBinding

class PermissionAdapter(
    private val items: List<PermissionItem>,
    private val onItemClick: (PermissionItem) -> Unit
) : RecyclerView.Adapter<PermissionAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPermissionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PermissionItem) {
            binding.tvName.text = item.name
            binding.tvDesc.text = item.description
            binding.ivIcon.setImageResource(item.iconRes)

            val drawable = binding.ivIconBg.background.mutate()
            drawable.setTint(ContextCompat.getColor(binding.root.context, item.iconTint))
            binding.ivIconBg.background = drawable

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPermissionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
