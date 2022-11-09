package eu.vendeli.tgbot.interfaces

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.types.Update
import eu.vendeli.tgbot.types.User
import eu.vendeli.tgbot.types.internal.ProcessedUpdate

interface Event {

    val bot: TelegramBot
    val update: ProcessedUpdate

    val fullUpdate: Update

    var isCancelled: Boolean
    var isHandled: Boolean

    val isCooldown: Boolean

    val user: User

    val chatId: Long

    val chatIsGroup: Boolean


    fun getShareData(key: String): Any?

    fun putSharedData(key: String, value: Any?)

}