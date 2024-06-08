package com.example.chronostimetracker

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class CategoryAdapter(private val categories: MutableList<String>, private val databaseRef: DatabaseReference) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryNameTextView: TextView = itemView.findViewById(R.id.categoryNameTextView)
        val btnDeleteCategory: Button = itemView.findViewById(R.id.btnDeleteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.categoryNameTextView.text = category

        holder.btnDeleteCategory.setOnClickListener {
            // Show confirmation dialog
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete this category?")
                .setPositiveButton("Yes") { dialog, _ ->
                    deleteCategory(position)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    private fun deleteCategory(position: Int) {
        val category = categories[position]

        // Find the category key by querying the database
        databaseRef.orderByValue().equalTo(category).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (childSnapshot in snapshot.children) {
                    childSnapshot.ref.removeValue().addOnSuccessListener {
                        Log.d("CategoryDelete", "Category deleted successfully")
                        categories.removeAt(position)
                        notifyItemRemoved(position)
                    }.addOnFailureListener {
                        Log.e("CategoryDelete", "Error deleting category")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CategoryDelete", "Error deleting category: ${error.message}")
            }
        })
    }
}

