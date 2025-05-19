package ai.koog.book.app.service

import ai.koog.book.app.model.Product
import ai.koog.book.app.model.ProductCatalog
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * A class that manages an online shop with product catalog and shopping cart functionality.
 */
class WebShopService private constructor() {

    companion object {

        private val logger = LoggerFactory.getLogger(WebShopService::class.java)

        val instance: WebShopService by lazy { WebShopService() }
    }

    private val catalogue = mutableListOf<Product>()
    private val cart = mutableListOf<Product>()
    private var onAddProductToCart: suspend (Product) -> Unit = {}

    init {
        loadProductsFromJson()
    }

    fun onAddProductToCart(action: suspend (Product) -> Unit) {
        val initialAction = onAddProductToCart
        onAddProductToCart = { product ->
            initialAction(product)
            action(product)
        }
    }

    /**
     * Adds a product to the shopping cart.
     *
     * @param product The product to add to the cart
     */
    suspend fun addProductToCart(product: Product) {
        cart.add(product)
        onAddProductToCart(product)
    }

    /**
     * Removes a product from the shopping cart by its ID.
     *
     * @param productId The ID of the product to remove
     */
    fun removeProductFromCart(productId: Int) {
        cart.removeIf { product -> product.id == productId }
    }

    /**
     * Gets a product in the catalogue by its ID.
     *
     * @param productId The ID of the product to find
     * @return The product if found, null otherwise
     */
    fun getProductInCatalogue(productId: Int): Product? {
        return catalogue.firstOrNull { product -> product.id == productId }
    }

    /**
     * Empties the shopping cart by removing all products.
     */
    fun emptyCart() {
        cart.clear()
    }

    /**
     * Gets the current contents of the shopping cart.
     *
     * @return A list of products in the cart
     */
    fun getCartContent(): List<Product> {
        return cart.toList()
    }

    /**
     * Searches for products in the catalogue by name.
     *
     * @param query The term to search for in product names
     * @return A list of products matching the search term
     */
    fun searchProductsInCatalogue(query: String): List<Product> {
        return catalogue.filter { product ->
            product.name.contains(query, ignoreCase = true) ||
                    levenshteinDistance(query.lowercase(), product.name.lowercase()) <= 5
        }
    }

    /**
     * Gets all products available in the catalogue.
     *
     * @return A list of all products
     */
    fun getAllProductsFromCatalogue(): List<Product> {
        return catalogue.toList()
    }

    //region Private Methods

    /**
     * Loads product data from a JSON file into the catalogue.
     */
    private fun loadProductsFromJson() {
        try {
            val jsonFile = this::class.java.classLoader.getResource("products.json")
            if (jsonFile != null) {
                val jsonContent = jsonFile.readText()
                val productCatalog = Json.decodeFromString<ProductCatalog>(jsonContent)
                catalogue.addAll(productCatalog.products)
                logger.info("Loaded <${catalogue.size}> products from JSON")
            }
            else {
                logger.error("Resource file 'products.json' not found")
            }
        }
        catch (t: Throwable) {
            logger.error("Error loading products from products.json: ${t.message}")
        }
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     *
     * @param query The first string to compare.
     * @param productName The second string to compare.
     * @return The Levenshtein distance between the two strings.
     */
    private fun levenshteinDistance(query: String, productName: String): Int {
        val m = query.length
        val n = productName.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) {
            dp[i][0] = i
        }
        for (j in 0..n) {
            dp[0][j] = j
        }

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (query[i - 1] == productName[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    minOf(
                        dp[i - 1][j] + 1,     // deletion
                        dp[i][j - 1] + 1,     // insertion
                        dp[i - 1][j - 1] + 1  // substitution
                    )
                }
            }
        }

        return dp[m][n]
    }

    //endregion Private Methods
}