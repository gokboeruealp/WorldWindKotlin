package earth.worldwind.globe.elevation.coverage

import earth.worldwind.geom.Angle
import earth.worldwind.geom.Location
import earth.worldwind.geom.Sector
import earth.worldwind.util.LruMemoryCache
import kotlinx.datetime.Clock

abstract class AbstractElevationCoverage: ElevationCoverage {
    override var displayName: String? = null
    override var isEnabled = true
        set(value) {
            field = value
            updateTimestamp()
        }
    override var timestamp = Clock.System.now()
        protected set
    private var userProperties: MutableMap<Any, Any>? = null
    private val heightCache = LruMemoryCache<Location,Float>(50000)

    protected fun updateTimestamp() {
        timestamp = Clock.System.now()
        heightCache.clear() // Invalidate cache if elevation coverage changed
    }

    override fun getUserProperty(key: Any) = userProperties?.get(key)

    override fun putUserProperty(key: Any, value: Any): Any? {
        val userProperties = userProperties ?: mutableMapOf<Any, Any>().also { userProperties = it }
        return userProperties.put(key, value)
    }

    override fun removeUserProperty(key: Any) = userProperties?.remove(key)

    override fun hasUserProperty(key: Any) = userProperties?.containsKey(key) == true

    override fun getHeight(latitude: Angle, longitude: Angle, retrieve: Boolean): Float? {
        return if (isEnabled) {
            val location = Location(latitude, longitude)
            heightCache[location] ?: doGetHeight(latitude, longitude, retrieve)?.also {
                heightCache.put(location, it, 1)
            }
        } else null
    }

    override fun getHeightGrid(gridSector: Sector, gridWidth: Int, gridHeight: Int, result: FloatArray) {
        if (isEnabled) doGetHeightGrid(gridSector, gridWidth, gridHeight, result)
    }

    override fun getHeightLimits(sector: Sector, result: FloatArray) {
        if (isEnabled) doGetHeightLimits(sector, result)
    }

    protected abstract fun doGetHeight(latitude: Angle, longitude: Angle, retrieve: Boolean): Float?

    protected abstract fun doGetHeightGrid(gridSector: Sector, gridWidth: Int, gridHeight: Int, result: FloatArray)

    protected abstract fun doGetHeightLimits(sector: Sector, result: FloatArray)
}