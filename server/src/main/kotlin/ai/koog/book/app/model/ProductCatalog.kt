package ai.koog.book.app.model

import kotlinx.serialization.Serializable

/**
 * Represents a catalog of products.
 *
 * @property products A list of products available in the catalog.
 */
@Serializable
data class ProductCatalog(val products: List<Product>)
