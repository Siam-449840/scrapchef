package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.EmojiFoodBeverage
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

// ═══════════════════════════════════════════════════════════════════
// IngredientIconService — Centralized Ingredient Icon Mapping
// ═══════════════════════════════════════════════════════════════════
// Single source of truth for ingredient visual representation.
// Every composable that displays an ingredient MUST resolve its icon
// through this service. Never assign ingredient icons ad-hoc.
//
// Resolution order:
//   1. Exact match (normalized lowercase)
//   2. Substring match (first keyword found in name)
//   3. GENERAL fallback (Restaurant icon)
// ═══════════════════════════════════════════════════════════════════

object IngredientIconService {

    enum class IngredientCategory(val icon: ImageVector) {
        VEGETABLE(Icons.Outlined.Spa),          // Herb/leaf — versatile for vegetables
        FRUIT(Icons.Outlined.LocalFlorist),     // Floral — closest to fruit
        MEAT(Icons.Outlined.SetMeal),           // Plated set meal
        SEAFOOD(Icons.Outlined.SetMeal),        // Reuse SetMeal — no fish icon in Outlined set
        DAIRY(Icons.Outlined.Egg),              // Egg covers dairy category
        GRAIN(Icons.Outlined.Grain),            // Grain icon — exact match
        SPICE(Icons.Outlined.LocalFlorist),     // Floral — represents dried botanicals
        HERB(Icons.Outlined.Grass),             // Grass — closest to fresh herbs
        SAUCE(Icons.Outlined.Opacity),          // Water drop — represents liquid sauces
        OIL_FAT(Icons.Outlined.WaterDrop),      // Distinct from Opacity for oils
        BEVERAGE(Icons.Outlined.LocalDrink),    // Drink cup
        GENERAL(Icons.Outlined.Restaurant),     // Neutral fallback — dining fork+knife
    }

    // Keyword → category mapping.
    // Keys must be lowercase. Resolution uses exact match then substring match.
    private val keywordMap: Map<String, IngredientCategory> = buildMap {
        // Vegetables
        listOf(
            "onion", "garlic", "tomato", "potato", "carrot", "bell pepper", "capsicum",
            "broccoli", "spinach", "lettuce", "cucumber", "zucchini", "courgette",
            "eggplant", "aubergine", "mushroom", "corn", "maize", "peas", "celery",
            "cabbage", "kale", "bok choy", "leek", "fennel", "artichoke", "asparagus",
            "beetroot", "beet", "turnip", "radish", "pumpkin", "squash", "sweet potato",
            "yam", "parsnip", "shallot", "chive", "spring onion", "scallion"
        ).forEach { put(it, IngredientCategory.VEGETABLE) }

        // Fruits
        listOf(
            "lemon", "lime", "orange", "apple", "banana", "mango", "pineapple",
            "strawberry", "blueberry", "raspberry", "blackberry", "cherry", "grape",
            "avocado", "peach", "plum", "apricot", "watermelon", "melon", "kiwi",
            "coconut", "papaya", "guava", "pomegranate", "fig", "date", "tamarind"
        ).forEach { put(it, IngredientCategory.FRUIT) }

        // Meat & Poultry
        listOf(
            "chicken", "beef", "pork", "lamb", "turkey", "duck", "goat", "veal",
            "bacon", "ham", "sausage", "salami", "prosciutto", "chorizo",
            "ground meat", "ground beef", "minced meat", "steak", "ribs",
            "liver", "kidney", "offal"
        ).forEach { put(it, IngredientCategory.MEAT) }

        // Seafood
        listOf(
            "shrimp", "prawn", "salmon", "tuna", "fish", "cod", "tilapia", "trout",
            "mackerel", "sardine", "anchovy", "crab", "lobster", "clam", "mussel",
            "oyster", "scallop", "squid", "calamari", "octopus"
        ).forEach { put(it, IngredientCategory.SEAFOOD) }

        // Dairy & Eggs
        listOf(
            "milk", "cheese", "butter", "cream", "yogurt", "yoghurt", "sour cream",
            "cream cheese", "mozzarella", "parmesan", "cheddar", "ricotta", "feta",
            "egg", "eggs", "ghee", "condensed milk", "evaporated milk", "whipping cream"
        ).forEach { put(it, IngredientCategory.DAIRY) }

        // Grains & Starches
        listOf(
            "rice", "pasta", "flour", "bread", "noodle", "noodles", "oat", "oats",
            "quinoa", "tortilla", "couscous", "barley", "wheat", "rye", "corn flour",
            "cornstarch", "cornmeal", "semolina", "polenta", "bulgur", "lentil",
            "lentils", "chickpea", "chickpeas", "bean", "beans", "dal", "dhal"
        ).forEach { put(it, IngredientCategory.GRAIN) }

        // Spices
        listOf(
            "cumin", "paprika", "turmeric", "cinnamon", "chili", "cayenne", "nutmeg",
            "coriander seed", "cardamom", "clove", "star anise", "bay leaf", "pepper",
            "black pepper", "white pepper", "fenugreek", "mustard seed", "allspice",
            "saffron", "sumac", "za'atar", "five spice", "garam masala", "curry powder"
        ).forEach { put(it, IngredientCategory.SPICE) }

        // Herbs (fresh/dried leaf herbs)
        listOf(
            "basil", "oregano", "thyme", "rosemary", "parsley", "cilantro", "coriander",
            "mint", "dill", "tarragon", "sage", "chervil", "marjoram", "lemongrass",
            "kaffir lime", "pandan"
        ).forEach { put(it, IngredientCategory.HERB) }

        // Sauces & Condiments
        listOf(
            "soy sauce", "vinegar", "ketchup", "tomato sauce", "mustard", "hot sauce",
            "worcestershire", "fish sauce", "sriracha", "hoisin", "oyster sauce",
            "teriyaki", "bbq sauce", "tahini", "miso", "soy", "tamari",
            "coconut aminos", "ponzu"
        ).forEach { put(it, IngredientCategory.SAUCE) }

        // Oils & Fats
        listOf(
            "olive oil", "vegetable oil", "coconut oil", "sesame oil", "canola oil",
            "sunflower oil", "avocado oil", "peanut oil", "lard", "shortening",
            "margarine", "oil"
        ).forEach { put(it, IngredientCategory.OIL_FAT) }

        // Beverages & Liquids
        listOf(
            "water", "stock", "broth", "wine", "beer", "coconut milk", "coconut water",
            "milk tea", "juice", "coffee", "tea"
        ).forEach { put(it, IngredientCategory.BEVERAGE) }
    }

    /**
     * Returns the appropriate Material icon for the given ingredient name.
     * Resolution is case-insensitive. Falls back to Restaurant icon if unknown.
     */
    fun iconFor(ingredientName: String): ImageVector {
        val normalized = ingredientName.trim().lowercase()

        // 1. Exact match
        keywordMap[normalized]?.let { return it.icon }

        // 2. Substring match — find first keyword contained in the ingredient name
        keywordMap.entries.firstOrNull { (keyword, _) ->
            normalized.contains(keyword)
        }?.value?.let { return it.icon }

        // 3. Fallback
        return IngredientCategory.GENERAL.icon
    }
}
