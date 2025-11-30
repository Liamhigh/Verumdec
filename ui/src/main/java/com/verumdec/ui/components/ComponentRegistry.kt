package com.verumdec.ui.components

/**
 * ComponentRegistry - Registry for UI components.
 */
class ComponentRegistry {

    private val components = mutableMapOf<String, ComponentInfo>()

    init {
        registerDefaultComponents()
    }

    private fun registerDefaultComponents() {
        register("evidence_card", ComponentInfo(
            name = "EvidenceCard",
            description = "Card displaying evidence item",
            category = ComponentCategory.CARD
        ))
        
        register("timeline_event", ComponentInfo(
            name = "TimelineEvent",
            description = "Timeline event display",
            category = ComponentCategory.LIST_ITEM
        ))
        
        register("contradiction_badge", ComponentInfo(
            name = "ContradictionBadge",
            description = "Contradiction severity indicator",
            category = ComponentCategory.BADGE
        ))
        
        register("entity_profile", ComponentInfo(
            name = "EntityProfile",
            description = "Entity profile card",
            category = ComponentCategory.CARD
        ))
        
        register("liability_gauge", ComponentInfo(
            name = "LiabilityGauge",
            description = "Liability score visualization",
            category = ComponentCategory.CHART
        ))
        
        register("progress_indicator", ComponentInfo(
            name = "ProgressIndicator",
            description = "Processing progress display",
            category = ComponentCategory.INDICATOR
        ))
        
        register("empty_state", ComponentInfo(
            name = "EmptyState",
            description = "Empty content placeholder",
            category = ComponentCategory.PLACEHOLDER
        ))

        register("constitution_warning", ComponentInfo(
            name = "ConstitutionWarning",
            description = "Constitution violation warning",
            category = ComponentCategory.ALERT
        ))
    }

    /**
     * Register a component.
     */
    fun register(id: String, info: ComponentInfo) {
        components[id] = info
    }

    /**
     * Get component info.
     */
    fun get(id: String): ComponentInfo? {
        return components[id]
    }

    /**
     * Get all components.
     */
    fun getAll(): Map<String, ComponentInfo> {
        return components.toMap()
    }

    /**
     * Get components by category.
     */
    fun getByCategory(category: ComponentCategory): List<ComponentInfo> {
        return components.values.filter { it.category == category }
    }
}

enum class ComponentCategory {
    CARD, LIST_ITEM, BADGE, CHART, INDICATOR, PLACEHOLDER, BUTTON, ALERT
}

data class ComponentInfo(
    val name: String,
    val description: String,
    val category: ComponentCategory
)
