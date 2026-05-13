package com.hasiru.usiru.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.hasiru.usiru.data.TreeTag
import com.hasiru.usiru.data.TreeTagDao
import kotlinx.coroutines.tasks.await

class FirebaseTreeSync(
    private val dao: TreeTagDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun pushUnsynced() {
        val pending = dao.unsynced()
        if (pending.isEmpty()) return

        val batch = firestore.batch()
        pending.forEach { tag ->
            val doc = firestore.collection("community_tree_map").document(tag.id.toString())
            batch.set(doc, tag.toCloudMap())
        }
        batch.commit().await()
        dao.markSynced(pending.map { it.id })
    }

    private fun TreeTag.toCloudMap(): Map<String, Any?> = mapOf(
        "type" to type.name,
        "latitude" to latitude,
        "longitude" to longitude,
        "accuracyMeters" to accuracyMeters,
        "species" to species?.name,
        "girthCm" to girthCm,
        "health" to health.name,
        "oxygenScore" to oxygenScore,
        "notes" to notes,
        "createdAt" to createdAt
    )
}
