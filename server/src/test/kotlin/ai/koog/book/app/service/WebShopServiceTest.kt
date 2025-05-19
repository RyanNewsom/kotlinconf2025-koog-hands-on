package ai.koog.book.app.service

import ai.koog.book.app.model.Product
import kotlinx.coroutines.test.runTest
import org.junit.After
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebShopServiceTest {

    val webShop = WebShopService.instance

    @After
    fun tearDown() {
        webShop.emptyCart()
    }

    @Test
    fun testProductLoading() = runTest {

        val allProducts = webShop.getAllProductsFromCatalogue()
        assertTrue(allProducts.isNotEmpty(), "Product catalogue should not be empty")

        if (allProducts.size >= 5) {
            val firstFiveProducts = allProducts.take(5)
            assertEquals(5, firstFiveProducts.size, "Should retrieve 5 products")

            firstFiveProducts.forEach { product ->
                assertTrue(product.id > 0, "Product ID should be positive")
                assertTrue(product.name.isNotEmpty(), "Product name should not be empty")
                assertTrue(product.price >= 0, "Product price should be non-negative")
            }
        }
    }

    @Test
    fun testSearchFunctionality() = runTest {

        val searchTerm = "tomato"
        val searchResults: List<Product> = webShop.searchProductsInCatalogue(query = searchTerm)

        assertTrue(
            searchResults.isNotEmpty(),
            "Search for '$searchTerm' should return results"
        )

        val expectedProducts = listOf(
            Product(id = 2, name = "Tomatoes BIO", price = 2.49),
            Product(id = 7, name = "Avocado", price = 2.99),
            Product(id = 14, name = "Cherry Tomatoes", price = 3.49),
            Product(id = 20, name = "Lemon", price = 0.79),
            Product(id = 21, name = "Heirloom Tomato Purple", price = 3.99),
            Product(id = 22, name = "Cherry Tomato Yellow", price = 3.29),
            Product(id = 23, name = "Roma Tomato Organic", price = 2.89),
            Product(id = 69, name = "Tomato Green Zebra", price = 4.29),
            Product(id = 70, name = "Tomato Kumato Brown", price = 4.99)
        )

        assertEquals(expectedProducts.size, searchResults.size)
        assertContentEquals(expectedProducts, searchResults)
    }

    @Test
    fun testBasketInitialState() = runTest {
        assertTrue(
            webShop.getCartContent().isEmpty(),
            "Basket should be empty initially"
        )
    }

    @Test
    fun testBasketFunctionality() = runTest {
        assertTrue(webShop.getCartContent().isEmpty())

        webShop.addProductToCart(webShop.getProductInCatalogue(1)!!)
        webShop.addProductToCart(webShop.getProductInCatalogue(2)!!)
        webShop.addProductToCart(webShop.getProductInCatalogue(8)!!)

        val basketContents = webShop.getCartContent()
        assertEquals(3, basketContents.size, "Basket should contain 3 items")

        val productIds = basketContents.map { it.id }
        assertTrue(productIds.contains(1), "Basket should contain product with ID 1")
        assertTrue(productIds.contains(2), "Basket should contain product with ID 2")
        assertTrue(productIds.contains(8), "Basket should contain product with ID 8")
    }

    @Test
    fun testRemoveFromBasket() = runTest {
        webShop.addProductToCart(webShop.getProductInCatalogue(1)!!)
        webShop.addProductToCart(webShop.getProductInCatalogue(2)!!)

        webShop.removeProductFromCart(1)

        val basketContents = webShop.getCartContent()
        assertEquals(1, basketContents.size, "Basket should contain 1 item after removal")
        assertEquals(2, basketContents[0].id, "Remaining item should have ID 2")
    }

    @Test
    fun testEmptyCart() = runTest {
        webShop.addProductToCart(webShop.getProductInCatalogue(1)!!)
        webShop.emptyCart()

        assertEquals(0, webShop.getCartContent().size, "Basket should be empty after emptying")
    }

    @Test
    fun testFindProduct() = runTest {
        val product = webShop.getProductInCatalogue(1)

        assertTrue(product != null, "Should find product with ID 1")
        assertEquals(1, product.id, "Product should have ID 1")
        assertTrue(product.name.isNotEmpty(), "Product should have a name")
    }

    @Test
    fun testFindNonExistingProduct() = runTest {
        val nonExistentProduct = webShop.getProductInCatalogue(9999)
        assertEquals(null, nonExistentProduct, "Should return null for non-existent product")
    }
}
