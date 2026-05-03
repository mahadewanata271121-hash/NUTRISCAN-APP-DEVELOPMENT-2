package com.example.nutriscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class FavoritesActivity : AppCompatActivity() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var emptyState: View
    private lateinit var btnCheckAll: ImageButton
    private lateinit var btnUncheckAll: ImageButton
    private lateinit var layoutDeleteActions: View
    private lateinit var btnDeleteSelected: MaterialButton
    private lateinit var btnResetFavorites: MaterialButton
    private lateinit var btnBackToHistory: MaterialButton
    private var adapter: FavoritesAdapter? = null
    private var isDeleteMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        rvFavorites = findViewById(R.id.rv_favorites)
        emptyState = findViewById(R.id.empty_favorites_state)
        btnCheckAll = findViewById(R.id.btn_check_all)
        btnUncheckAll = findViewById(R.id.btn_uncheck_all)
        layoutDeleteActions = findViewById(R.id.layout_delete_actions)
        btnDeleteSelected = findViewById(R.id.btn_delete_selected)
        btnResetFavorites = findViewById(R.id.btn_reset_favorites)
        btnBackToHistory = findViewById(R.id.btn_back_to_history)

        findViewById<ImageButton>(R.id.btn_back_favorites).setOnClickListener {
            finish()
        }

        btnBackToHistory.setOnClickListener {
            val intent = Intent(this, AnalysisHistoryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        btnResetFavorites.setOnClickListener {
            resetAllFavorites()
        }

        btnDeleteSelected.setOnClickListener {
            deleteSelectedFavorites()
        }

        btnCheckAll.setOnClickListener {
            adapter?.selectAll()
            updateDeleteButtonsState()
        }

        btnUncheckAll.setOnClickListener {
            adapter?.unselectAll()
            updateDeleteButtonsState()
        }

        setupRecyclerView()
        setupSwipeToCancel()
    }

    private fun setupRecyclerView() {
        rvFavorites.layoutManager = LinearLayoutManager(this)
        loadFavorites()
    }

    private fun loadFavorites() {
        val sharedPref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        val favoritesData = sharedPref.getString("favorite_foods", "")
        
        if (favoritesData.isNullOrEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvFavorites.visibility = View.GONE
            layoutDeleteActions.visibility = View.GONE
            exitDeleteMode()
        } else {
            emptyState.visibility = View.GONE
            rvFavorites.visibility = View.VISIBLE
            
            val items = favoritesData.split("#").filter { it.isNotEmpty() }.mapNotNull {
                val parts = it.split("|")
                try {
                    if (parts.size >= 6) {
                        ScannedFood(parts[0], parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), parts[4].toInt(), parts[5].toInt())
                    } else if (parts.size >= 5) {
                        ScannedFood(parts[0], parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), parts[4].toInt(), R.drawable.illustration22)
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            
            if (adapter == null) {
                adapter = FavoritesAdapter(items.toMutableList(), { enterDeleteMode() }, { updateDeleteButtonsState() })
                rvFavorites.adapter = adapter
            } else {
                adapter?.updateData(items)
            }
            
            if (isDeleteMode) {
                enterDeleteMode() // Refresh UI states
            }
        }
    }

    private fun updateDeleteButtonsState() {
        adapter?.let { adapter ->
            val items = adapter.getCurrentItems()
            val selectedCount = items.count { it.isSelected }
            val totalCount = items.size

            if (selectedCount == 0) {
                // Initial or nothing selected
                btnDeleteSelected.alpha = 0.5f
                btnDeleteSelected.isEnabled = false
                btnResetFavorites.alpha = 0.5f
                btnResetFavorites.isEnabled = false
            } else if (selectedCount == totalCount) {
                // All selected
                btnDeleteSelected.alpha = 0.3f
                btnDeleteSelected.isEnabled = false
                btnResetFavorites.alpha = 1.0f
                btnResetFavorites.isEnabled = true
            } else {
                // Some selected
                btnDeleteSelected.alpha = 1.0f
                btnDeleteSelected.isEnabled = true
                btnResetFavorites.alpha = 0.5f
                btnResetFavorites.isEnabled = false
            }
        }
    }

    private fun enterDeleteMode() {
        isDeleteMode = true
        btnCheckAll.visibility = View.VISIBLE
        btnUncheckAll.visibility = View.VISIBLE
        layoutDeleteActions.visibility = View.VISIBLE
        adapter?.setDeleteMode(true)
        updateDeleteButtonsState()
    }

    private fun exitDeleteMode() {
        isDeleteMode = false
        btnCheckAll.visibility = View.GONE
        btnUncheckAll.visibility = View.GONE
        layoutDeleteActions.visibility = View.GONE
        adapter?.setDeleteMode(false)
        adapter?.unselectAll()
    }

    private fun deleteSelectedFavorites() {
        adapter?.let { adapter ->
            val items = adapter.getCurrentItems()
            val remainingItems = items.filter { !it.isSelected }
            
            val sharedPref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
            val dataToSave = remainingItems.joinToString("#") { 
                "${it.name}|${it.cal}|${it.prot}|${it.carbs}|${it.fat}|${it.imageRes}"
            }
            sharedPref.edit().putString("favorite_foods", dataToSave).apply()
            
            loadFavorites()
            Toast.makeText(this, "Item terpilih dihapus", Toast.LENGTH_SHORT).show()
            
            if (remainingItems.isEmpty()) {
                exitDeleteMode()
            }
        }
    }

    private fun setupSwipeToCancel() {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 != null && e2.x - e1.x > 100) { // Swipe Right
                    if (isDeleteMode) {
                        exitDeleteMode()
                        return true
                    }
                }
                return false
            }
        })
        rvFavorites.setOnTouchListener { v, event -> 
            gestureDetector.onTouchEvent(event)
            v.performClick()
            false
        }
    }

    private fun resetAllFavorites() {
        val sharedPref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
        sharedPref.edit().putString("favorite_foods", "").apply()
        exitDeleteMode()
        loadFavorites()
        Toast.makeText(this, "Semua favorit telah dihapus", Toast.LENGTH_SHORT).show()
    }

    data class ScannedFood(val name: String, val cal: Int, val prot: Int, val carbs: Int, val fat: Int, val imageRes: Int, var isSelected: Boolean = false)

    class FavoritesAdapter(
        private var items: MutableList<ScannedFood>, 
        private val onLongPress: () -> Unit,
        private val onSelectionChanged: () -> Unit
    ) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {
        
        private var deleteMode = false

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvFoodName)
            val tvDetails: TextView = view.findViewById(R.id.tvNutrientDetails)
            val ivFood: ImageView = view.findViewById(R.id.iv_food_icon)
            val checkbox: CheckBox = view.findViewById(R.id.cb_favorite_select)
        }

        fun updateData(newItems: List<ScannedFood>) {
            this.items.clear()
            this.items.addAll(newItems)
            notifyDataSetChanged()
        }

        fun getCurrentItems(): List<ScannedFood> = items

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scanned_food, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvDetails.text = "${item.cal} kcal | P: ${item.prot}g | K: ${item.carbs}g | L: ${item.fat}g"
            holder.ivFood.setImageResource(item.imageRes)
            
            holder.checkbox.visibility = if (deleteMode) View.VISIBLE else View.GONE

            holder.checkbox.setOnCheckedChangeListener(null)
            holder.checkbox.isChecked = item.isSelected
            holder.checkbox.setOnCheckedChangeListener { _, isChecked -> 
                item.isSelected = isChecked 
                onSelectionChanged()
            }

            holder.itemView.setOnLongClickListener {
                onLongPress()
                true
            }
        }

        override fun getItemCount() = items.size

        fun setDeleteMode(enabled: Boolean) {
            deleteMode = enabled
            notifyDataSetChanged()
        }

        fun selectAll() {
            items.forEach { it.isSelected = true }
            notifyDataSetChanged()
        }

        fun unselectAll() {
            items.forEach { it.isSelected = false }
            notifyDataSetChanged()
        }
    }
}
