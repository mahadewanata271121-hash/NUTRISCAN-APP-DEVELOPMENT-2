package com.example.nutriscan

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class DateAdapter(private val dates: List<DateItem>) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val date = dates[position]
        holder.bind(date)

        if (date.isSelected) {
            // 1. Ganti background menjadi OVAL ORANYE
            holder.itemView.setBackgroundResource(R.drawable.bg_date_selected)

            // 2. Ganti warna teks Hari & Tanggal menjadi PUTIH
            holder.dayNameText.setTextColor(Color.WHITE)
            holder.dayNumberText.setTextColor(Color.WHITE)
        } else {
            // Tampilan saat tidak dipilih
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)

            // Gunakan warna hitam (atau warna sesuai kebutuhan) bukannya Color.GRAY yang fix abu-abu
            val context = holder.itemView.context
            val unselectedColor = ContextCompat.getColor(context, R.color.black)

            holder.dayNameText.setTextColor(unselectedColor)
            holder.dayNumberText.setTextColor(unselectedColor)
        }
    }

    override fun getItemCount(): Int = dates.size

    inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayNameText: TextView = itemView.findViewById(R.id.day_name_text)
        val dayNumberText: TextView = itemView.findViewById(R.id.day_number_text)

        fun bind(dateItem: DateItem) {
            val dayNameFormat = SimpleDateFormat("EEE", Locale("id", "ID"))
            val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())

            dayNameText.text = dayNameFormat.format(dateItem.date.time)
            dayNumberText.text = dayNumberFormat.format(dateItem.date.time)
        }
    }
}
