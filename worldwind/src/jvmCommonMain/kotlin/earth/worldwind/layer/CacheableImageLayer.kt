package earth.worldwind.layer

import earth.worldwind.shape.TiledSurfaceImage
import earth.worldwind.util.ContentManager

interface CacheableImageLayer : Layer {
    /**
     * Main tiled surface image used to represent this layer
     */
    val tiledSurfaceImage: TiledSurfaceImage?
    /**
     * Unique key of this layer in the cache
     */
    var contentKey: String?
    /**
     * Checks if cache is successfully configured
     */
    val isCacheConfigured get() = tiledSurfaceImage?.cacheTileFactory != null
    /**
     * Allows saving new content to cache
     */
    var isCacheWritable: Boolean
        get() = tiledSurfaceImage?.cacheTileFactory?.isWritable ?: false
        set(value) { tiledSurfaceImage?.cacheTileFactory?.isWritable = value }
    /**
     * Configures tiled image layer to retrieve a cache source only
     */
    var isCacheOnly: Boolean
        get() = tiledSurfaceImage?.isCacheOnly ?: false
        set(value) { tiledSurfaceImage?.isCacheOnly = value }

    /**
     * Configures image layer to use specified cache provider
     *
     * @param contentManager Cache content manager
     * @param contentKey Content key inside the specified content manager
     *
     * @throws IllegalArgumentException In case of incompatible level set configured in cache content
     * @throws IllegalStateException In the case of cache configuration requested on a read-only content
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    suspend fun configureCache(contentManager: ContentManager, contentKey: String) {
        contentManager.setupImageLayerCache(this, contentKey)
    }

    /**
     * Deletes all tiles from current cache storage
     *
     * @param deleteMetadata also delete cache metadata
     * @throws IllegalStateException In case of read-only database
     */
    @Throws(IllegalStateException::class)
    suspend fun clearCache(deleteMetadata: Boolean = false) = tiledSurfaceImage?.cacheTileFactory?.clearContent(deleteMetadata).also {
        if (deleteMetadata) disableCache()
    }

    /**
     * Removes cache provider from the current tiled image layer
     */
    fun disableCache() {
        tiledSurfaceImage?.cacheTileFactory = null
    }
}