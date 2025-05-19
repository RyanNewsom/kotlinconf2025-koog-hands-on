package ai.koog.book.agent

import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall

/**
 * The [CookingAgentStrategies] object contains various strategies
 * for creating and executing cooking-related workflows for an AI agent.
 */
object CookingAgentStrategies {

    /**
     * This strategy for the example purposes only
     */
    fun exampleStrategy(): AIAgentStrategy = strategy("example") {
        // The start node of the strategy is String -> String node.
        // It accepts and returns agent input
        println(nodeStart.name)

        // The finish node of the strategy is also String -> String node.
        // It accepts the incoming input and returns it as an agent result
        println(nodeFinish.name)

        // You can call an LLM node with/without tools.
        val nodeRequestLLM by nodeLLMRequest(name = "Request LLM", allowToolCalls = false)
        println(nodeRequestLLM.name)

        // To connect nodes use edge(fromNode forwardTo toNode)
        edge(nodeStart forwardTo nodeFinish transformed { it + "Visited edge from start to finish" })

        // You can also use onCondition to define conditions for the edge
        edge(nodeStart forwardTo nodeFinish onCondition { it.contains("Hello") })

        // You can use onToolCall predefined condition which will check if the llm output is a tool call
        edge(nodeRequestLLM forwardTo nodeFinish onToolCall { true } transformed { "Tool message: ${it.tool}" })

        // You can use onAssistantMessage predefined condition which will check if the llm output is an assistant message
        edge(nodeRequestLLM forwardTo nodeFinish onAssistantMessage { true } transformed { "Assistant message: $it" })
    }
}
