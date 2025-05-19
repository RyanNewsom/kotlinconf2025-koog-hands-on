package ai.koog.book.app.server

/**
 * Configuration data class for the `KoogBookServer`.
 *
 * @property host The hostname or IP address where the server will run. Defaults to `127.0.0.1`.
 * @property port The port number on which the server will listen to. Defaults to `5991`.
 */
data class KoogServerConfig(
    val host: String = "127.0.0.1",
    val port: Int = 5991,
)
