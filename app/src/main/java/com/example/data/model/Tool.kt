package com.example.data.model


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
