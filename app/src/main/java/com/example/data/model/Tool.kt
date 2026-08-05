package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Microwave
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

enum class Tool(val id: String, val displayName: String) {
    STOVETOP("stovetop", "Stovetop"),
    OVEN("oven", "Oven"),
    MICROWAVE("microwave", "Microwave"),
    AIR_FRYER("airfryer", "Air Fryer"),
    NO_COOK("no-cook", "No-Cook");

    companion object {
        fun fromId(id: String): Tool {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: STOVETOP
        }
    }
}
