package com.hasiru.usiru.data

enum class TreeHealth(val label: String, val multiplier: Double) {
    HEALTHY("Healthy", 1.0),
    NEEDS_CARE("Needs care", 0.7),
    DRY("Dry", 0.25)
}
