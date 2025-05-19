package ai.koog.book.app.server

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.net.ServerSocket
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KoogBookServerTest {

    private val clientBaseUrl = "http://127.0.0.1"

    @Test
    fun testCookServerIsStarted() = runTest {

        val port = findAvailablePort()
        val serverConfig = KoogServerConfig(port = port)

        KoogBookServer(serverConfig).use { server ->
            server.startServer(wait = false)

            // Wait for the server to start
            delay(1000)

            HttpClient().use { client ->
                val response = client.get("$clientBaseUrl:$port/healthcheck")
                assertEquals(HttpStatusCode.Companion.OK, response.status)
            }
        }
    }

    @Test
    @Ignore("Test require OPEN_AI_TOKEN variable")
    fun testCookServerEndpoint() = runTest {
        // Check if OpenAI token is set
        val openAiToken = System.getenv("OPEN_AI_TOKEN")
        if (openAiToken.isNullOrEmpty()) {
            println("Skipping test because OPEN_AI_TOKEN environment variable is not set")
            return@runTest
        }

        val port = findAvailablePort()
        val serverConfig = KoogServerConfig(port = port)

        KoogBookServer(serverConfig).use { server ->
            server.startServer(wait = false)

            // Wait for the server to start
            delay(1000)

            HttpClient().use { client ->
                val userInput = "I would like to make a salad for dinner"

                // Make a GET request to the cook endpoint with the input as a query parameter
                val response = client.get("$clientBaseUrl:$port/cook") {
                    url {
                        parameters.append("input", userInput)
                    }
                }

                // Verify that the response status is OK
                assertEquals(HttpStatusCode.OK, response.status)

                // Verify that the response body is not empty
                val responseBody = response.bodyAsText()
                assertTrue(responseBody.isNotEmpty(), "Response body should not be empty")

                // The cook endpoint should return a Server-Sent Events stream
                // We can't easily parse the SSE stream in a test, but we can verify that
                // the response contains expected event types
                assertTrue(responseBody.contains("event: assistant") || 
                           responseBody.contains("event: toolCall") ||
                           responseBody.contains("event: ingredients"), 
                           "Response should contain at least one event type")
                assertTrue(responseBody.contains("event: finish"), "Response should contain a finish event")
            }
        }
    }

    @Test
    @Ignore("Test require OPEN_AI_TOKEN variable")
    fun testSSEEvents() = runTest {
        // Check if OpenAI token is set
        val openAiToken = System.getenv("OPEN_AI_TOKEN")
        if (openAiToken.isNullOrEmpty()) {
            println("Skipping test because OPEN_AI_TOKEN environment variable is not set")
            return@runTest
        }

        val port = findAvailablePort()
        val serverConfig = KoogServerConfig(port = port)

        KoogBookServer(serverConfig).use { server ->
            server.startServer(wait = false)

            // Wait for the server to start
            delay(1000)

            HttpClient().use { client ->
                // Test the cart endpoint
                val cartResponse = client.get("$clientBaseUrl:$port/cart")
                assertEquals(HttpStatusCode.OK, cartResponse.status)

                // Test the products endpoint
                val productsResponse = client.get("$clientBaseUrl:$port/products")
                assertEquals(HttpStatusCode.OK, productsResponse.status)

                // Test the cart/clear endpoint
                val clearCartResponse = client.post("$clientBaseUrl:$port/cart/clear")
                assertEquals(HttpStatusCode.OK, clearCartResponse.status)
            }
        }
    }

    //region Private Methods

    private fun findAvailablePort(): Int {
        val port = ServerSocket(0).use { socket ->
            socket.localPort
        }

        return port
    }

    //endregion Private Methods
}
