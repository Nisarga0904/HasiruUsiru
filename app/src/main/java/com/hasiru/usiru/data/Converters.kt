package com.hasiru.usiru.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun toTreeType(value: String): TreeType = TreeType.valueOf(value)
    @TypeConverter fun fromTreeType(value: TreeType): String = value.name

    @TypeConverter fun toTreeHealth(value: String): TreeHealth = TreeHealth.valueOf(value)
    @TypeConverter fun fromTreeHealth(value: TreeHealth): String = value.name

    @TypeConverter fun toSpecies(value: String?): Species? = value?.let(Species::valueOf)
    @TypeConverter fun fromSpecies(value: Species?): String? = value?.name
}
