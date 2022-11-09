package eu.vendeli.tgbot.types.internal

import eu.vendeli.tgbot.enums.MethodPriority
import java.lang.reflect.Method

data class Invocation(
    val clazz: Class<*>,
    val method: Method,
    val namedParameters: Map<String, String> = mapOf(),
    val priority: MethodPriority = MethodPriority.NORMAL,
    val ignoreCancelled: Boolean = false,
    val isAsync: Boolean = false,
    val cooldown: Long = 0,
    val ignoreCooldown: Boolean = false
)
