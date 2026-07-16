package com.shahriarhasan.usedphoneinspector.core.model

import androidx.annotation.StringRes
import com.shahriarhasan.usedphoneinspector.R

data class PhysicalChecklistDefinition(
    val categoryKey: String,
    @StringRes val categoryLabel: Int,
    val items: List<PhysicalItemDefinition>,
)

data class PhysicalItemDefinition(val key: String, @StringRes val label: Int)

object PhysicalChecklist {
    val categories = listOf(
        category("display_front", R.string.physical_display_front,
            "screen_scratches" to R.string.physical_screen_scratches,
            "cracks" to R.string.physical_cracks,
            "screen_separation" to R.string.physical_screen_separation,
            "burn_in" to R.string.physical_burn_in,
            "uneven_brightness" to R.string.physical_uneven_brightness,
            "replaced_screen" to R.string.physical_replaced_screen,
            "front_camera_glass" to R.string.physical_front_camera_glass,
            "earpiece_grille" to R.string.physical_earpiece_grille,
            "proximity_area" to R.string.physical_proximity_area,
        ),
        category("frame_body", R.string.physical_frame_body,
            "frame_dents" to R.string.physical_frame_dents,
            "bent_chassis" to R.string.physical_bent_chassis,
            "paint_damage" to R.string.physical_paint_damage,
            "loose_back" to R.string.physical_loose_back,
            "back_glass_cracks" to R.string.physical_back_glass_cracks,
            "camera_bump" to R.string.physical_camera_bump,
            "missing_parts" to R.string.physical_missing_parts,
            "visible_glue" to R.string.physical_visible_glue,
            "uneven_gaps" to R.string.physical_uneven_gaps,
            "repair_marks" to R.string.physical_repair_marks,
            "screws" to R.string.physical_screws,
        ),
        category("buttons", R.string.physical_buttons,
            "power_button" to R.string.physical_power_button,
            "volume_up" to R.string.physical_volume_up,
            "volume_down" to R.string.physical_volume_down,
            "extra_buttons" to R.string.physical_extra_buttons,
            "fingerprint_button" to R.string.physical_fingerprint_button,
            "button_looseness" to R.string.physical_button_looseness,
            "button_response" to R.string.physical_button_response,
        ),
        category("ports_trays", R.string.physical_ports,
            "charging_port" to R.string.physical_charging_port,
            "headphone_port" to R.string.physical_headphone_port,
            "speaker_grille" to R.string.physical_speaker_grille,
            "microphone_holes" to R.string.physical_microphone_holes,
            "sim_tray" to R.string.physical_sim_tray,
            "memory_slot" to R.string.physical_memory_slot,
            "port_corrosion" to R.string.physical_port_corrosion,
            "loose_charging" to R.string.physical_loose_charging,
        ),
        category("cameras_glass", R.string.physical_cameras,
            "rear_camera_glass" to R.string.physical_rear_camera_glass,
            "front_camera_glass_2" to R.string.physical_front_camera_glass,
            "lens_scratches" to R.string.physical_lens_scratches,
            "lens_cracks" to R.string.physical_lens_cracks,
            "internal_dust" to R.string.physical_internal_dust,
            "lens_condensation" to R.string.physical_lens_condensation,
        ),
        category("water_repair", R.string.physical_water_repair,
            "water_indicator" to R.string.physical_water_indicator,
            "corrosion" to R.string.physical_corrosion,
            "moisture" to R.string.physical_moisture,
            "aftermarket_parts" to R.string.physical_aftermarket_parts,
            "opened_device" to R.string.physical_opened_device,
            "seller_repairs" to R.string.physical_seller_repairs,
            "repair_invoice" to R.string.physical_repair_invoice,
        ),
        category("accessories", R.string.physical_accessories,
            "box" to R.string.accessory_box,
            "charger" to R.string.accessory_charger,
            "cable" to R.string.accessory_cable,
            "case" to R.string.accessory_case,
            "earphones" to R.string.accessory_earphones,
            "warranty" to R.string.accessory_warranty,
            "receipt" to R.string.accessory_receipt,
            "sim_tool" to R.string.accessory_sim_tool,
        ),
    )

    private fun category(
        key: String,
        label: Int,
        vararg items: Pair<String, Int>,
    ) = PhysicalChecklistDefinition(key, label, items.map { PhysicalItemDefinition(it.first, it.second) })
}

