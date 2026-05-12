package com.example.nutriscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private val notifications: List<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.iv_notif_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_notif_title)
        val tvMessage: TextView = view.findViewById(R.id.tv_notif_message)
        val tvTime: TextView = view.findViewById(R.id.tv_notif_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notifications[position]
        holder.tvTitle.text = notif.title
        holder.tvMessage.text = notif.message
        holder.tvTime.text = notif.time

        when (notif.type) {
            "ALERT" -> {
                holder.ivIcon.setImageResource(R.drawable.lonceng_notif)
                holder.ivIcon.setTint(ContextCompat.getColor(holder.itemView.context, R.color.orange_primary))
            }
            "REMINDER" -> {
                holder.ivIcon.setImageResource(R.drawable.lonceng_notif)
                holder.ivIcon.setTint(ContextCompat.getColor(holder.itemView.context, R.color.orange_primary))
            }
            "TIP" -> {
                holder.ivIcon.setImageResource(R.drawable.edukasi)
                holder.ivIcon.setTint(ContextCompat.getColor(holder.itemView.context, R.color.orange_primary))
            }
        }
    }

    private fun ImageView.setTint(color: Int) {
        this.setColorFilter(color)
    }

    override fun getItemCount() = notifications.size
}
