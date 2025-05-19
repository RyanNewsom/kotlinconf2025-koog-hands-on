package ai.koog.book.app.server

import ai.koog.book.app.model.*
import ai.koog.book.app.service.AgentService
import ai.koog.book.app.service.WebShopService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory
import java.lang.AutoCloseable

class KoogBookServer(private val config: KoogServerConfig) : AutoCloseable {

    companion object {
        private val logger = LoggerFactory.getLogger(KoogBookServer::class.java)
    }

    private val server = embeddedServer(CIO, host = config.host, port = config.port) {
        configureServer()
    }

    private val agentService = AgentService()

    //region Start / Stop

    fun startServer(wait: Boolean = true) {
        // Init Web Shop API before starting a server
        WebShopService.instance

        server.start(wait = wait)
        logger.info("Server started on port ${config.port}")
    }

    override fun close() {
        server.stop(1000, 1000)
        logger.info("Server stopped")
    }

    //endregion Start / Stop

    //region Private Methods

    private fun Application.configureServer() {

        install(DefaultHeaders)
        install(ContentNegotiation) {
            json(defaultJson)
        }
        install(SSE)

        configureRouting()
    }

    private fun Application.configureRouting(): Routing =
        routing {
            staticResources("/", "static") { default("index.html") }
            staticResources("/static/image", "static/image")

            post("/cancel") {

            }

            // Cook SSE endpoint - starts agent and returns ingredients based on user input
            sse("/cook") {
                val userInput = call.request.queryParameters["input"] ?: ""
                logger.info("SSE cook request with input: $userInput")

                WebShopService.instance.onAddProductToCart { product ->
                    sendAddProductToCartMessage(product)
                }

                // Start the agent
                try {
                    agentService.startAgent(userInput) { message: Message ->
                        sendAgentMessage(message)
                    }
                }
                catch (t: Throwable) {
                    sendServerErrorMessage(t.message ?: "Unknown error")
                }

                sendFinishMessage()
            }

            get("/healthcheck") {
                call.respond(HttpStatusCode.OK, "Koog Book Server is running")
            }

            get("/cart") {
                call.respond(WebShopService.instance.getCartContent())
            }

            get("/products") {
                call.respond(WebShopService.instance.getAllProductsFromCatalogue())
            }

            post("/cart/remove") {
                val productId = call.request.queryParameters["id"]?.toIntOrNull()
                if (productId != null) {
                    WebShopService.instance.removeProductFromCart(productId)
                    call.respond(HttpStatusCode.OK, "Product removed from cart")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Invalid product ID")
                }
            }

            post("/cart/clear") {
                WebShopService.instance.emptyCart()
                call.respond(HttpStatusCode.OK, "Cart cleared")
            }

            post("/cart/add") {
                val productId = call.request.queryParameters["id"]?.toIntOrNull()
                if (productId != null) {
                    val product = WebShopService.instance.getProductInCatalogue(productId)
                    if (product != null) {
                        WebShopService.instance.addProductToCart(product)
                        call.respond(HttpStatusCode.OK, "Product added to cart")
                    } else {
                        call.respond(HttpStatusCode.BadRequest, "Product with ID $productId not found")
                    }
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Invalid product ID")
                }
            }
        }

    private suspend fun ServerSSESession.sendAgentMessage(message: Message) {
        val serverEvent = ServerSentEvent(
            event = message.messageType.event,
            data = message.toServerEventData()
        )

        sendSSEMessage(serverEvent)
    }

    private suspend fun ServerSSESession.sendAddProductToCartMessage(product: Product) {
        val message = AddProductToCartMessage(product = product)

        val serverEvent = ServerSentEvent(
            event = message.messageType.event,
            data = defaultJson.encodeToString(product)
        )

        sendSSEMessage(serverEvent)
    }

    private suspend fun ServerSSESession.sendServerErrorMessage(error: String) {
        val message = ServerErrorMessage(message = error)
        val serverEvent = ServerSentEvent(
            event = message.messageType.event,
            data = message.toServerEventData()
        )
        sendSSEMessage(serverEvent)
    }

    private suspend fun ServerSSESession.sendFinishMessage() {
        val serverEvent = ServerSentEvent(
            event = FinishMessage.messageType.event,
            data = ""
        )
        sendSSEMessage(serverEvent)
    }

    private suspend fun ServerSSESession.sendSSEMessage(event: ServerSentEvent) {
        try {
            send(event)
        }
        catch (t: CancellationException) {
            logger.info("SSE stream cancelled")
            throw t
        }
        catch (t: Throwable) {
            logger.error("Error sending SSE event: ${t.message}", t)
        }
    }

    private fun Message.toServerEventData(): String {
        return defaultJson.encodeToString(this)
    }

    //endregion Private Methods
}
