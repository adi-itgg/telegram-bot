package eu.vendeli.tgbot.events

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.interfaces.Event
import eu.vendeli.tgbot.types.Update
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.ProcessedUpdate

internal class EventUpdate(
    override val bot: TelegramBot,
    override val update: ProcessedUpdate
): Event {

    private val sharedData = mutableMapOf<String, Any?>()
    var _isCooldown = false

    override val fullUpdate: Update
        get() = update.fullUpdate

    override val user: User
        get() = update.user

    override val chatId: Long
        get() = update.fullUpdate.message?.chat?.id ?: fullUpdate.callbackQuery?.message?.chat?.id ?: update.user.id

    override val chatIsGroup: Boolean
        get() = chatId < 0

    override var isCancelled = false
    override var isHandled = false

    override val isCooldown: Boolean
        get() = _isCooldown

    override fun getShareData(key: String): Any? = sharedData[key]

    override fun putSharedData(key: String, value: Any?) {
        sharedData[key] = value
    }

}