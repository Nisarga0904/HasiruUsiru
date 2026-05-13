package com.hasiru.usiru

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import com.hasiru.usiru.data.TreeTag
import com.hasiru.usiru.data.TreeType

class TreeClusterItem(private val tag: TreeTag) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(tag.latitude, tag.longitude)
    override fun getTitle(): String = if (tag.type == TreeType.EMPTY_PIT) "Empty pit" else tag.species?.commonName ?: "Tree"
    override fun getSnippet(): String = if (tag.type == TreeType.EMPTY_PIT) "Needs planting" else "Oxygen score ${"%.1f".format(tag.oxygenScore)}"
    override fun getZIndex(): Float = 0f
}
