package eu.vendeli.tgbot.core

import eu.vendeli.tgbot.annotations.CallbackParam
import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.annotations.UnprocessedHandler
import eu.vendeli.tgbot.types.internal.Actions
import eu.vendeli.tgbot.types.internal.Invocation
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.lang.reflect.Parameter

/**
 * Collects commands and inputs before the program starts in a particular package by key annotations.
 *
 */
internal object TelegramActionsCollector {
    private fun Array<Parameter>.getParameters() = filter { it.annotations.any { a -> a is CallbackParam } }
        .associate { p -> p.name to (p.annotations.first { it is CallbackParam } as CallbackParam).name }

    /**
     * Function that collects a list of actions that the bot will operate with.
     *
     * @param packageName the name of the package where the search will take place.
     * @return [Actions]
     */
    fun collect(packageName: String): Actions = with(
        Reflections(
            packageName,
            Scanners.MethodsAnnotated
        )
    ) {
        val commands = mutableMapOf<String, Invocation>()
        val inputs = mutableMapOf<String, Invocation>()
        val unhandleds = mutableListOf<Invocation>()

        getMethodsAnnotatedWith(CommandHandler::class.java).forEach { m ->
            (m.annotations.find { it is CommandHandler } as CommandHandler).let {
                it.value.forEach { v ->
                    commands[v] = Invocation(
                        clazz = m.declaringClass,
                        method = m,
                        namedParameters = m.parameters.getParameters(),
                        cooldown = it.cooldown,
                        ignoreCooldown = it.ignoreCooldown
                    )
                }
            }
        }

        getMethodsAnnotatedWith(InputHandler::class.java).forEach { m ->
            (m.annotations.find { it is InputHandler } as InputHandler).value.forEach { v ->
                inputs[v] = Invocation(
                    clazz = m.declaringClass,
                    method = m,
                    namedParameters = m.parameters.getParameters()
                )
            }
        }

        getMethodsAnnotatedWith(UnprocessedHandler::class.java).forEach { m ->
            val anno = (m.annotations.find { it is UnprocessedHandler } as UnprocessedHandler)
            unhandleds.add(Invocation(
                clazz = m.declaringClass,
                method = m,
                namedParameters = m.parameters.getParameters(),
                priority = anno.priority,
                ignoreCancelled = anno.ignoreCancelled,
                isAsync = anno.isAsync
            ))
        }

        return@with Actions(
            commands = commands,
            inputs = inputs,
            unhandled = unhandleds
        )
    }
}
