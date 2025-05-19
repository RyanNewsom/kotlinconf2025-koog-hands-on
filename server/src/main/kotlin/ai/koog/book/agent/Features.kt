package ai.koog.book.agent

import ai.koog.agents.core.agent.AIAgent.FeatureContext
import ai.koog.agents.core.tools.reflect.ToolFromCallable.VarArgs
import ai.koog.agents.local.features.eventHandler.feature.EventHandler
import ai.koog.agents.local.features.tracing.feature.Tracing
import ai.koog.book.app.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

fun FeatureContext.configureFeatures(onAgentEvent: suspend (Message) -> Unit) {
    install(Tracing)

    install(EventHandler) {
        onToolCallResult = { tool, toolArgs, result ->
            val argsMessage = (toolArgs as? VarArgs)?.asNamedValues()?.toMap()?.toString() ?: toolArgs.toString()
            val resultMessage = result?.toStringDefault() ?: "UNKNOWN TOOL CALL RESULT"
            val escapedResultMessage = try {
                Json.decodeFromString<JsonPrimitive>(resultMessage).jsonPrimitive.content
            } catch (_: Exception) {
                resultMessage
            }
            val message = LLMToolCallMessage(
                toolName = tool.name,
                toolArgs = argsMessage,
                result = escapedResultMessage
            )
            onAgentEvent(message)
        }

        var isFirstLLMCall = true

        onAfterLLMWithToolsCall = { response, tools ->
            if (isFirstLLMCall) {
                isFirstLLMCall = false
                val ingredients = response.firstOrNull()?.getIngredients() ?: emptyList()
                val message = IngredientsMessage(
                    ingredients = ingredients
                )
                onAgentEvent(message)
            }

            val messageBuilder = StringBuilder()
            messageBuilder.appendLine("LLM Responses:")
            response.forEach { responseMessage -> messageBuilder.appendLine("  - ${responseMessage.content}") }

            if (tools.isNotEmpty()) {
                messageBuilder
                    .append("Tools: ")
                    .append("[")
                    .append(tools.joinToString { it.name })
                    .append("]")
                    .appendLine()
            }

            val message = LLMMessage(message = messageBuilder.toString())
            onAgentEvent(message)
        }

        onAgentFinished = { strategyName: String, result: String? ->
            val message = LLMMessage(
                message = "Agent finished with result: $result"
            )
            onAgentEvent(message)
        }

        onAgentRunError = { strategyName, throwable ->
            val message = LLMErrorMessage(
                message = "Agent execution error: ${throwable.message}"
            )
            onAgentEvent(message)
        }
    }
}

//region Private Methods and Operators

private val ingredientItemTrimRegex = "^[\\d\\s\\-.,;: ]*".toRegex()
private val ingredientItemRegex = "^[\\d-+•*]".toRegex()

private fun ai.koog.prompt.message.Message.Response.getIngredients(): List<String> {
    // Select only the list of ingredients
    val trimmedContent = content.lines().filter { line ->
        ingredientItemRegex.containsMatchIn(line.trim())
    }

    return trimmedContent.map { ingredient ->
        ingredientItemTrimRegex.replaceFirst(ingredient.trim(), "")
    }
}

//endregion Private Methods and Operators