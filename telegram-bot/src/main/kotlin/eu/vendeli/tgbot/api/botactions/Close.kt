package eu.vendeli.tgbot.api.botactions

import eu.vendeli.tgbot.interfaces.SimpleAction
import eu.vendeli.tgbot.types.internal.TgMethod

/**
 * Use this method to close the bot instance before moving it from one local server to another. You need to delete the webhook before calling this method to ensure that the bot isn't launched again after server restart.
 * The method will return error 429 in the first 10 minutes after the bot is launched. Returns True on success.
 *
 */
class CloseAction : SimpleAction<Boolean> {
    override val method: TgMethod = TgMethod("close")
    override val parameters: MutableMap<String, Any?> = mutableMapOf()
}

/**
 * Use this method to close the bot instance before moving it from one local server to another. You need to delete the webhook before calling this method to ensure that the bot isn't launched again after server restart.
 * The method will return error 429 in the first 10 minutes after the bot is launched. Returns True on success.
 *
 */
fun close() = CloseAction()
