package ai.koog.book.app.service

import ai.koog.book.agent.CookingAgent
import ai.koog.book.app.model.Message
import org.slf4j.LoggerFactory

/**
 * A service class to manage and execute a cooking agent process.
 * This class handles the initialization and interaction with the `CookingAgent`.
 */
class AgentService() {

    companion object {
        private val logger = LoggerFactory.getLogger(AgentService::class.java)
    }

    /**
     * Starts a cooking agent with the provided cooking request and handles its events.
     *
     * @param cookingRequest A string representing the cooking request that the agent will execute.
     * @param onAgentEvent A suspendable callback function to handle events emitted by the cooking agent.
     */
    suspend fun startAgent(cookingRequest: String, onAgentEvent: suspend (Message) -> Unit) = try {
        logger.info("Starting agent with request: $cookingRequest")
        val agent = CookingAgent()
        agent.execute(cookingRequest, onAgentEvent)
    }
    catch (t: Throwable) {
        logger.error("Error running agent: ${t.message}", t)
        throw t
    }
}
