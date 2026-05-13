package com.hasiru.usiru.data

import androidx.annotation.DrawableRes
import com.hasiru.usiru.R

enum class Species(
    val commonName: String,
    val kannadaName: String,
    val scientificName: String,
    val soil: String,
    val descriptionKannada: String,
    val factor: Double,
    @DrawableRes val imageRes: Int
) {
    NEEM(
        commonName = "Neem",
        kannadaName = "ಬೇವು",
        scientificName = "Azadirachta indica",
        soil = "Red loam, sandy loam, well-drained urban soil",
        descriptionKannada = "ಬೇವು ನೆರಳು, ಔಷಧೀಯ ಗುಣ ಮತ್ತು ಕಡಿಮೆ ನೀರಿನ ಅಗತ್ಯಕ್ಕಾಗಿ ಸೂಕ್ತವಾದ ಸ್ಥಳೀಯ ಮರ.",
        factor = 1.18,
        imageRes = R.drawable.guide_neem
    ),
    HONGE(
        commonName = "Honge",
        kannadaName = "ಹೊಂಗೆ",
        scientificName = "Millettia pinnata",
        soil = "Clay loam, tank-bed edges, compacted roadside soil",
        descriptionKannada = "ಹೊಂಗೆ ರಸ್ತೆಬದಿ ನೆಡುವಿಕೆಗೆ ಬಲವಾದ ಮರ; ಮಣ್ಣು ಸುಧಾರಣೆ ಮತ್ತು ಜೈವ ವೈವಿಧ್ಯಕ್ಕೆ ಸಹಕಾರಿ.",
        factor = 1.34,
        imageRes = R.drawable.guide_honge
    ),
    PEEPAL(
        commonName = "Peepal",
        kannadaName = "ಅರಳಿ",
        scientificName = "Ficus religiosa",
        soil = "Deep alluvial or loamy soil with root space",
        descriptionKannada = "ಅರಳಿ ದೊಡ್ಡ ನೆರಳು ನೀಡುವ ಪವಿತ್ರ ಸ್ಥಳೀಯ ಮರ; ವಿಶಾಲ ಜಾಗದಲ್ಲಿ ನೆಡುವುದು ಉತ್ತಮ.",
        factor = 1.55,
        imageRes = R.drawable.guide_peepal
    );

    companion object {
        fun simulatedFromPhoto(seed: String): Species {
            val all = entries
            return all[kotlin.math.abs(seed.hashCode()) % all.size]
        }
    }
}
