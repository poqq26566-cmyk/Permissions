package com.example.permissionmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.permissionmanager.databinding.ItemAppBinding

class AppListAdapter(
    private val items: List<AppPermInfo>,
    /** 是否展示"已授权/未授权"标签。全部应用模式（如耗电行为管理、剪贴板）
     *  没有权限授予的概念，这个标签没有意义，直接隐藏。 */
    private val showGrantedStatus: Boolean = true,
    private val onItemClick: (AppPermInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppPermInfo) {
            binding.tvAppName.text = item.label
            binding.tvAppPackage.text = item.packageName
            binding.ivAppIcon.setImageDrawable(item.icon)

            if (!showGrantedStatus) {
                binding.tvStatus.visibility = View.GONE
            } else {
                binding.tvStatus.visibility = View.VISIBLE
                if (item.granted) {
                    binding.tvStatus.text = "已授权"
                    binding.tvStatus.setTextColor(0xFF4CAF50.toInt())
                } else {
                    binding.tvStatus.text = "未授权"
                    binding.tvStatus.setTextColor(0xFF9E9E9E.toInt())
                }
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
