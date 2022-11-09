package eu.vendeli.tgbot.core

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.TelegramBot.Companion.mapper
import eu.vendeli.tgbot.api.deleteMessage
import eu.vendeli.tgbot.enums.MethodPriority
import eu.vendeli.tgbot.events.EventUpdate
import eu.vendeli.tgbot.interfaces.BotInputListener
import eu.vendeli.tgbot.interfaces.ClassManager
import eu.vendeli.tgbot.interfaces.Event
import eu.vendeli.tgbot.types.Update
import eu.vendeli.tgbot.types.internal.*
import eu.vendeli.tgbot.utils.CreateNewCoroutineContext
import eu.vendeli.tgbot.utils.invokeSuspend
import eu.vendeli.tgbot.utils.parseQuery
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.CoroutineContext

/**
 * A class that handles updates.
 *
 * @property actions The list of actions the handler will work with.
 * @property bot An instance of [TelegramBot]
 * @property classManager An instance of the class that will be used to call functions.
 * @property inputListener An instance of the class that stores the input waiting points.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class TelegramUpdateHandler internal constructor(
    private val actions: Actions? = null,
    private val bot: TelegramBot,
    private val classManager: ClassManager,
    private val inputListener: BotInputListener,
) {
    private val logger = LoggerFactory.getLogger(this::class.java.simpleName)
    private lateinit var listener: suspend TelegramUpdateHandler.(Update) -> Unit
    private var handlerActive: Boolean = false
    private val manualHandlingBehavior by lazy { ManualHandlingDsl(bot, inputListener) }

    private val cooldowns = mutableMapOf<Long, Long>()

    /**
     * Function that starts the listening event.
     *
     * @param offset
     */
    private tailrec suspend fun runListener(offset: Int? = null): Int {
        logger.trace("Running listener with offset - $offset")
        if (!handlerActive) {
            coroutineContext.cancelChildren()
            return 0
        }
        var lastUpdateId: Int = offset ?: 0
        bot.pullUpdates(offset)?.forEach {
            CreateNewCoroutineContext(coroutineContext).launch(bot.defaultContext) {
                listener(this@TelegramUpdateHandler, it)
            }
            lastUpdateId = it.updateId + 1
        }
        delay(100)
        return runListener(lastUpdateId)
    }

    /**
     * Function to define the actions that will be applied to updates when they are being processed.
     * When set, it starts an update processing cycle.
     *
     * @param block action that will be applied.
     * @receiver [CoroutineContext]
     */
    suspend fun setListener(block: suspend TelegramUpdateHandler.(Update) -> Unit) {
        if (handlerActive) stopListener()
        logger.trace("The listener is set.")
        listener = block
        handlerActive = true
        runListener()
    }

    /**
     * Stops listening of new updates.
     *
     */
    fun stopListener() {
        logger.trace("The listener is stopped.")
        handlerActive = false
    }

    /**
     * Function for mapping text with a specific command or input.
     *
     * @param text
     * @param command true to search in commands or false to search among inputs. Default - true.
     * @return [Activity] if actions was found or null.
     */
    private fun findAction(text: String, command: Boolean = true): Activity? {
        val message = text.parseQuery()
        val invocation = (
            if (command) actions?.commands else {
                actions?.inputs
            }
            )?.get(message.command)
        return if (invocation != null) Activity(invocation = invocation, parameters = message.params) else null
    }

    /**
     * Updates parsing method
     *
     * @param update
     * @return [Update] or null
     */
    fun parseUpdate(update: String): Update? {
        logger.trace("Trying to parse update from string - $update")
        return mapper.runCatching {
            readValue(update, Update::class.java)
        }.onFailure {
            logger.debug("error during the update parsing process.", it)
        }.onSuccess { logger.trace("Successfully parsed update to $it") }.getOrNull()
    }

    /**
     * Updates parsing method
     *
     * @param updates
     * @return [Update] or null
     */
    fun parseUpdates(updates: String): List<Update>? {
        logger.trace("Trying to parse bunch of updates from string - $updates")
        return mapper.runCatching {
            readValue(updates, jacksonTypeRef<Response<List<Update>>>()).getOrNull()
        }.onFailure {
            logger.debug("error during the bunch updates parsing process.", it)
        }.onSuccess { logger.trace("Successfully parsed updates to $it") }.getOrNull()
    }

    /**
     * [Update] extension function that helps to handle the update (annotations mode)
     *
     */
    @JvmName("handleIt")
    suspend fun Update.handle() = handle(this)

    /**
     * [Update] extension function that helps to handle the update (manual mode)
     *
     */
    @JvmName("handleItManually")
    suspend fun Update.handle(block: suspend ManualHandlingDsl.() -> Unit) = handle(this, block)

    /**
     * Function used to call functions with certain parameters processed after receiving update.
     *
     * @param event
     * @param invocation
     * @param parameters
     * @return null on success or [Throwable].
     */
    private suspend fun invokeMethod(
        event: Event,
        invocation: Invocation,
        parameters: Map<String, String>,
    ): Throwable? {
        val update = event.update
        var isSuspend = false
        logger.trace("Parsing arguments for Update#${update.fullUpdate.updateId}")
        val processedParameters = buildList {
            invocation.method.parameters.forEach { p ->
                if (p.type.name == "kotlin.coroutines.Continuation") {
                    isSuspend = true
                    return@forEach
                }
                val parameterName = invocation.namedParameters.getOrDefault(p.name, p.name)
                val typeName = p.parameterizedType.typeName
                if (parameters.keys.contains(parameterName)) when (p.parameterizedType.typeName) {
                    "java.lang.String" -> add(parameters[parameterName].toString())
                    "java.lang.Integer", "int" -> add(parameters[parameterName]?.toIntOrNull())
                    "java.lang.Long", "long" -> add(parameters[parameterName]?.toLongOrNull())
                    "java.lang.Short", "short" -> add(parameters[parameterName]?.toShortOrNull())
                    "java.lang.Float", "float" -> add(parameters[parameterName]?.toFloatOrNull())
                    "java.lang.Double", "double" -> add(parameters[parameterName]?.toDoubleOrNull())
                    else -> add(null)
                } else when {
                    typeName == "eu.vendeli.tgbot.types.User" -> add(update.user)
                    typeName == "eu.vendeli.tgbot.TelegramBot" -> add(bot)
                    typeName == "eu.vendeli.tgbot.types.internal.ProcessedUpdate" -> add(update)
                    typeName == "eu.vendeli.tgbot.interfaces.Event" -> add(event)
                    bot.magicObjects.contains(p.type) -> add(bot.magicObjects[p.type]?.get(update, bot))
                    else -> add(null)
                }
            }
        }

        bot.chatData?.run {
            logger.trace("Handling BotContext for Update#${update.fullUpdate.updateId}")
            if (!update.user.isPresent()) return@run
            val prevClassName = getAsync(update.user.id, "PrevInvokedClass").await()?.toString()
            if (prevClassName != invocation.clazz.name) delPrevChatSectionAsync(update.user.id).await()

            setAsync(update.user.id, "PrevInvokedClass", invocation.clazz.name).await()
        }

        logger.trace("Invoking function for Update#${update.fullUpdate.updateId}")
        invocation.runCatching {
            if (!invocation.ignoreCooldown && event.isCooldown) {
                logger.debug("User#${event.user.id} command is cooldown ${event.fullUpdate.message?.text ?: invocation}")
                val chatId = update.fullUpdate.message?.chat?.id ?: update.fullUpdate.callbackQuery?.message?.chat?.id ?: update.user.id
                if (chatId < 0)
                    deleteMessage(update.fullUpdate.message?.messageId ?: return null).send(chatId, bot)
                return null
            }
            if (isSuspend)
                if (invocation.isAsync)
                    CreateNewCoroutineContext(coroutineContext).launch {
                        method.invokeSuspend(classManager.getInstance(clazz), *processedParameters.toTypedArray())
                    }
                else
                    method.invokeSuspend(classManager.getInstance(clazz), *processedParameters.toTypedArray())
            else method.invoke(classManager.getInstance(clazz), *processedParameters.toTypedArray())
        }.onFailure {
            logger.debug("Method {$invocation} invocation error at handling update: $update", it)
            return it
        }.onSuccess { logger.debug("Handled update#${update.fullUpdate.updateId} to method ${invocation.clazz.simpleName}::${invocation.method.name}") }
        return null
    }

    /**
     * Handle the update.
     *
     * @param update
     * @return null on success or [Throwable].
     */
    suspend fun handle(update: Update): Throwable? = processUpdateDto(update).run {
        logger.trace("Handling update: $update")
        logger.debug("Handling update user ${update.message?.from?.firstName} - text: ${update.message?.text}")
        val commandAction = text?.run {
            // to fix command not detected if text contains @ like /rules@bot
            findAction(if (contains("@"))
                split("@")[0]
            else
                this)
        }
        val inputAction = if (commandAction == null) inputListener.getAsync(user.id).await()?.let {
            findAction(it, false)
        } else null
        logger.trace("Result of finding action - command: $commandAction, input: $inputAction")
        inputListener.delAsync(user.id).await()

        val event = EventUpdate(bot, this)

        return when {
            commandAction != null -> commandAction.run {
                event._isCooldown = cooldowns[user.id]?.let { date ->
                    if ((System.currentTimeMillis() - date) > invocation.cooldown) {
                        cooldowns[user.id] = System.currentTimeMillis()
                        false
                    } else true
                } ?: run {
                    cooldowns[user.id] = System.currentTimeMillis()
                    false
                }
                invokeMethod(event, invocation, parameters)
            }
            inputAction != null && update.message?.from?.isBot == false -> invokeMethod(
                event = event,
                invocation = inputAction.invocation,
                parameters = inputAction.parameters
            )

            actions?.unhandled?.isNotEmpty() == true -> {
                MethodPriority.values().sortedArrayDescending().forEach { priority ->
                    actions.unhandled.filter {
                        it.priority == priority
                    }.forEach {
                        if (!event.isCancelled || it.ignoreCancelled)
                            invokeMethod(event, it, emptyMap())
                        else
                            logger.debug("Cancelled update#${event.fullUpdate.updateId} to method ${it.clazz.simpleName}::${it.method.name}")
                    }
                }
                if (!event.isHandled && !event.isCancelled)
                    logger.info("update: ${update.updateId} not handled.")
                null
            }

            else -> {
                logger.info("update: ${update.updateId} not handled.")
                null
            }
        }
    }

    /**
     * Manual handling dsl
     *
     * @param update
     * @param block
     */
    suspend fun handle(update: Update, block: suspend ManualHandlingDsl.() -> Unit) {
        logger.trace("Manually handling update: $update")
        manualHandlingBehavior.apply {
            block()
            CreateNewCoroutineContext(coroutineContext).launch(Dispatchers.IO) {
                process(update)
            }
        }
    }
}
