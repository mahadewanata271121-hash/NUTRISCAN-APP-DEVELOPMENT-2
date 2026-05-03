package com.example.nutriscan

data class FoodNutrient(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

object NutrientDatabase {
    private val data = mapOf(
        "ikan mujair" to FoodNutrient("Ikan Mujair Goreng", 128, 18, 0, 8),
        "nasi putih" to FoodNutrient("Nasi Putih", 130, 2, 28, 0),
        "tahu goreng" to FoodNutrient("Tahu Goreng", 115, 8, 3, 7),
        "tempe goreng" to FoodNutrient("Tempe Goreng", 192, 11, 9, 12),
        "tumis kangkung" to FoodNutrient("Tumis Kangkung", 98, 3, 5, 8),
        "Tempe bacem" to FoodNutrient("Tempe Bacem", 165, 10, 15, 6),
        "bakso" to FoodNutrient("Bakso Kuah", 218, 12, 14, 15),
        "bika ambon" to FoodNutrient("Bika Ambon", 145, 2, 25, 6),
        "dadar gulung" to FoodNutrient("Dadar Gulung", 160, 3, 28, 5),
        "kue cubit" to FoodNutrient("Kue Cubit", 180, 4, 30, 6),
        "nasi goreng" to FoodNutrient("Nasi Goreng", 250, 7, 30, 12),
        "pepes ikan" to FoodNutrient("Pepes Ikan", 145, 20, 2, 6),
        "putu ayu" to FoodNutrient("Putu Ayu", 120, 2, 22, 4),
        "rendang" to FoodNutrient("Rendang Sapi", 195, 18, 4, 11),
        "sate ayam" to FoodNutrient("Sate Ayam", 225, 25, 5, 12),
        "telur balado" to FoodNutrient("Telur Balado", 145, 12, 3, 10)
    )

    fun getNutrient(label: String): FoodNutrient? {
        // Case-insensitive mapping
        return data[label] ?: data.entries.find { it.key.equals(label, ignoreCase = true) }?.value
    }
}
