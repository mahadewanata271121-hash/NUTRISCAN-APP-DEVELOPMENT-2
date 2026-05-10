package com.example.nutriscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * FavoritesActivity - Performance Optimized
 * Dioptimalkan untuk pengolahan data besar di background agar UI tetap responsif.
 */
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

        initViews()
        setupListeners()
        setupRecyclerView()
    }

    private fun initViews() {
        rvFavorites = findViewById(R.id.rv_favorites)
        emptyState = findViewById(R.id.empty_favorites_state)
        btnCheckAll = findViewById(R.id.btn_check_all)
        btnUncheckAll = findViewById(R.id.btn_uncheck_all)
        layoutDeleteActions = findViewById(R.id.layout_delete_actions)
        btnDeleteSelected = findViewById(R.id.btn_delete_selected)
        btnResetFavorites = findViewById(R.id.btn_reset_favorites)
        btnBackToHistory = findViewById(R.id.btn_back_to_history)
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btn_back_favorites).setOnClickListener { finish() }

        btnBackToHistory.setOnClickListener {
            startActivity(Intent(this, AnalysisHistoryActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            })
        }

        btnResetFavorites.setOnClickListener { resetAllFavorites() }
        btnDeleteSelected.setOnClickListener { deleteSelectedFavorites() }
        btnCheckAll.setOnClickListener { 
            adapter?.selectAll()
            updateDeleteButtonsState()
        }
        btnUncheckAll.setOnClickListener { 
            adapter?.unselectAll()
            updateDeleteButtonsState()
        }
    }

    private fun setupRecyclerView() {
        rvFavorites.layoutManager = LinearLayoutManager(this)
        loadFavorites()
        setupSwipeGesture()
    }

    private fun loadFavorites() {
        // Optimasi: Load data favorit di Background Thread
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPref = getSharedPreferences("UserStats", Context.MODE_PRIVATE)
            val favoritesData = sharedPref.getString("favorite_foods", "") ?: ""
            
            val items = if (favoritesData.isEmpty()) emptyList() else {
                favoritesData.split("#").filter { it.isNotEmpty() }.mapNotNull {
                    val parts = it.split("|")
                    try {
                        if (parts.size >= 6) {
                            ScannedFood(parts[0], parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), parts[4].toInt(), parts[5].toInt())
                        } else if (parts.size >= 5) {
                            ScannedFood(parts[0], parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), parts[4].toInt(), R.drawable.illustration22)
                        } else null
                    } catch (e: Exception) { null }
                }
            }

            withContext(Dispatchers.Main) {
                updateUIWithData(items)
            }
        }
    }

    private fun updateUIWithData(items: List<ScannedFood>) {
        if (items.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvFavorites.visibility = View.GONE
            layoutDeleteActions.visibility = View.GONE
            exitDeleteMode()
        } else {
            emptyState.visibility = View.GONE
            rvFavorites.visibility = View.VISIBLE
            
            if (adapter == null) {
                adapter = FavoritesAdapter(items.toMutableList(), { enterDeleteMode() }, { updateDeleteButtonsState() })
                rvFavorites.adapter = adapter
            } else {
                adapter?.updateData(items)
            }
            if (isDeleteMode) enterDeleteMode()
        }
    }

    private fun updateDeleteButtonsState() {
        adapter?.let { adp ->
            val items = adp.getCurrentItems()
            val selectedCount = items.count { it.isSelected }
            val totalCount = items.size

            btnDeleteSelected.isEnabled = selectedCount > 0 && selectedCount < totalCount
            btnDeleteSelected.alpha = if (btnDeleteSelected.isEnabled) 1.0f else 0.5f
            
            btnResetFavorites.isEnabled = selectedCount == totalCount
            btnResetFavorites.alpha = if (btnResetFavorites.isEnabled) 1.0f else 0.3f
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
        val items = adapter?.getCurrentItems() ?: return
        val remaining = items.filter { !it.isSelected }
        
        lifecycleScope.launch(Dispatchers.IO) {
            val dataToSave = remaining.joinToString("#") { 
                "${it.name}|${it.cal}|${it.prot}|${it.carbs}|${it.fat}|${it.imageRes}"
            }
            getSharedPreferences("UserStats", Context.MODE_PRIVATE).edit()
                .putString("favorite_foods", dataToSave).apply()
            
            withContext(Dispatchers.Main) {
                loadFavorites()
                Toast.makeText(this@FavoritesActivity, "Item dihapus", Toast.LENGTH_SHORT).show()
                if (remaining.isEmpty()) exitDeleteMode()
            }
        }
    }

    private fun resetAllFavorites() {
        getSharedPreferences("UserStats", Context.MODE_PRIVATE).edit()
            .putString("favorite_foods", "").apply()
        exitDeleteMode()
        loadFavorites()
        Toast.makeText(this, "Semua favorit dihapus", Toast.LENGTH_SHORT).show()
    }

    private fun setupSwipeGesture() {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 != null && e2.x - e1.x > 150 && isDeleteMode) { 
                    exitDeleteMode()
                    return true
                }
                return false
            }
        })
        rvFavorites.setOnTouchListener { v, event -> 
            detector.onTouchEvent(event)
            false 
        }
    }

    data class ScannedFood(val name: String, val cal: Int, val prot: Int, val carbs: Int, val fat: Int, val imageRes: Int, var isSelected: Boolean = false)

    class FavoritesAdapter(
        private var items: MutableList<ScannedFood>, 
        private val onLongPress: () -> Unit,
        private val onSelectionChanged: () -> Unit
    ) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {
        
        private var deleteMode = false

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvFoodName)
            val tvDetails: TextView = v.findViewById(R.id.tvNutrientDetails)
            val ivFood: ImageView = v.findViewById(R.id.iv_food_icon)
            val checkbox: CheckBox = v.findViewById(R.id.cb_favorite_select)
        }

        fun updateData(newItems: List<ScannedFood>) {
            this.items.clear()
            this.items.addAll(newItems)
            notifyDataSetChanged() // Optimasi DiffUtil bisa ditambahkan jika item > 100
        }

        fun getCurrentItems() = items

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = 
            ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_scanned_food, p, false))

        override fun onBindViewHolder(h: ViewHolder, pos: Int) {
            val item = items[pos]
            h.tvName.text = item.name
            h.tvDetails.text = "${item.cal} kcal | P: ${item.prot}g | K: ${item.carbs}g"
            h.ivFood.setImageResource(item.imageRes)
            
            h.checkbox.visibility = if (deleteMode) View.VISIBLE else View.GONE
            h.checkbox.setOnCheckedChangeListener(null)
            h.checkbox.isChecked = item.isSelected
            h.checkbox.setOnCheckedChangeListener { _, isChecked -> 
                item.isSelected = isChecked 
                onSelectionChanged()
            }

            h.itemView.setOnLongClickListener { onLongPress(); true }
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
