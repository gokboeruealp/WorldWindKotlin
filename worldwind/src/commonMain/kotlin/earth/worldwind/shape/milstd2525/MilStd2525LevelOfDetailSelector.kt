package earth.worldwind.shape.milstd2525

import earth.worldwind.render.RenderContext
import earth.worldwind.shape.Placemark
import earth.worldwind.shape.PlacemarkAttributes
import earth.worldwind.shape.milstd2525.MilStd2525.getSimplifiedSymbolID
import earth.worldwind.shape.milstd2525.MilStd2525.getUnfilledAttributes
import earth.worldwind.shape.milstd2525.MilStd2525.isTacticalGraphic
import earth.worldwind.shape.milstd2525.MilStd2525.modifiersThreshold
import earth.worldwind.shape.milstd2525.MilStd2525Placemark.Companion.getPlacemarkAttributes

/**
 * The [MilStd2525LevelOfDetailSelector] determines which set of [PlacemarkAttributes] to use for a [MilStd2525Placemark].
 * A [MilStd2525Placemark] creates an instance of this class in its constructor, and calls
 * [Placemark.LevelOfDetailSelector.selectLevelOfDetail] in its [Placemark.doRender] method.
 */
open class MilStd2525LevelOfDetailSelector : Placemark.LevelOfDetailSelector {
    protected var lastLevelOfDetail = -1
    protected var isHighlighted = false
    protected var isInvalidateRequested = false

    override fun invalidate() { isInvalidateRequested = true }

    /**
     * Gets the active attributes for the current distance to the camera and highlighted state.
     *
     * @param rc             The current render contents
     * @param placemark      The placemark needing a level of detail selection
     * @param cameraDistance The distance from the placemark to the camera (meters)
     *
     * @return if placemark should display or skip its rendering
     */
    override fun selectLevelOfDetail(rc: RenderContext, placemark: Placemark, cameraDistance: Double): Boolean {
        if (placemark !is MilStd2525Placemark) return true
        val isHighlightChanged = placemark.isHighlighted != isHighlighted
        isHighlighted = placemark.isHighlighted

        // Determine the normal attributes based on highlighted state and the distance from the camera to the placemark
        if (cameraDistance > placemark.eyeDistanceScalingThreshold && !placemark.isHighlighted) {
            // Low-fidelity: use a SIDC code with affiliation code only
            if (lastLevelOfDetail != LOW_LEVEL_OF_DETAIL || isInvalidateRequested) {
                val simpleCode = if (isTacticalGraphic(placemark.symbolCode))
                    getSimplifiedSymbolID(placemark.symbolCode)
                else placemark.symbolCode.substring(0, 3) + "*------*****"
                placemark.attributes = getPlacemarkAttributes(simpleCode, symbolAttributes = getAttributes(placemark))
                lastLevelOfDetail = LOW_LEVEL_OF_DETAIL
            }
        } else if (cameraDistance > modifiersThreshold && !placemark.isHighlighted || !placemark.isModifiersVisible) {
            // Medium-fidelity: use a simplified SIDC code without status, mobility, size and text modifiers
            if (lastLevelOfDetail != MEDIUM_LEVEL_OF_DETAIL || isInvalidateRequested) {
                val simpleCode = getSimplifiedSymbolID(placemark.symbolCode)
                placemark.attributes = getPlacemarkAttributes(simpleCode, symbolAttributes = getAttributes(placemark))
                lastLevelOfDetail = MEDIUM_LEVEL_OF_DETAIL
            }
        } else if (!placemark.isHighlighted) {
            // High-fidelity: use the regular SIDC code without text modifiers, except unique designation (T)
            if (lastLevelOfDetail != HIGH_LEVEL_OF_DETAIL || isInvalidateRequested) {
                val basicModifiers = placemark.symbolModifiers?.filter { (k,_) -> k == "T" }
                placemark.attributes = getPlacemarkAttributes(
                    placemark.symbolCode, basicModifiers, getAttributes(placemark)
                )
                lastLevelOfDetail = HIGH_LEVEL_OF_DETAIL
            }
        } else {
            // Highest-fidelity: use the regular SIDC code with all available text modifiers
            if (lastLevelOfDetail != HIGHEST_LEVEL_OF_DETAIL || isInvalidateRequested || isHighlightChanged) {
                placemark.attributes = getPlacemarkAttributes(
                    placemark.symbolCode, placemark.symbolModifiers, getAttributes(placemark)
                )
                lastLevelOfDetail = HIGHEST_LEVEL_OF_DETAIL
            }
        }

        isInvalidateRequested = false

        placemark.attributes.isDrawLeader = lastLevelOfDetail >= MEDIUM_LEVEL_OF_DETAIL
        placemark.attributes.imageScale = if (isHighlighted) HIGHLIGHTED_SCALE else NORMAL_SCALE
        placemark.attributes.labelAttributes.scale = if (isHighlighted) HIGHLIGHTED_SCALE else NORMAL_SCALE

        return true
    }

    private fun getAttributes(placemark: MilStd2525Placemark) = if (placemark.isFrameFilled) placemark.symbolAttributes
    else placemark.symbolAttributes?.let { getUnfilledAttributes(placemark.symbolCode) + it }
        ?: getUnfilledAttributes(placemark.symbolCode)

    companion object {
        protected const val NORMAL_SCALE = 1.0
        protected const val HIGHLIGHTED_SCALE = 1.3
        protected const val LOW_LEVEL_OF_DETAIL = 0
        protected const val MEDIUM_LEVEL_OF_DETAIL = 1
        protected const val HIGH_LEVEL_OF_DETAIL = 2
        protected const val HIGHEST_LEVEL_OF_DETAIL = 3
    }
}