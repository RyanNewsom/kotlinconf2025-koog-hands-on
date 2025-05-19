package ai.koog.book.app.model

/**
* Represents the different types of messages that can be handled within the system.
*
* @property event The string representation of the event associated with the message type.
*/
enum class MessageType(val event: String) {
    ASSISTANT("assistant"),
    TOOL_CALL("toolCall"),
    ASSISTANT_ERROR("assistantError"),
    INGREDIENTS("ingredients"),
    ADD_TO_CART("addToCart"),
    ERROR("error"),
    FINISH("finish"),
}
