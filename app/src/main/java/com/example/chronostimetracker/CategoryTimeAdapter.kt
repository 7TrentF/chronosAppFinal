package com.example.chronostimetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryTimeAdapter(private val categoryTimeList: List<CategoryTime>) :
    RecyclerView.Adapter<CategoryTimeAdapter.CategoryTimeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryTimeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_time, parent, false)
        return CategoryTimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryTimeViewHolder, position: Int) {
        val categoryTime = categoryTimeList[position]
        holder.tvCategory.text = "    ${categoryTime.category}"
        holder.tvTotalTime.text = categoryTime.formattedTime




    }

    override fun getItemCount(): Int = categoryTimeList.size

    class CategoryTimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvTotalTime: TextView = itemView.findViewById(R.id.tvTotalTime)
    }



}
