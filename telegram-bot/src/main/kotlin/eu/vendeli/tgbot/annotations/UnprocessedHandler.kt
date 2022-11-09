package eu.vendeli.tgbot.annotations

import eu.vendeli.tgbot.enums.MethodPriority

/**
 * Annotation used to mark the function that is used to handle updates that not processed.
 * Multiple processing point is possible.
 *
 * @param after method class + name
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UnprocessedHandler(
    val priority: MethodPriority = MethodPriority.NORMAL,
    val ignoreCancelled: Boolean = false,
    val isAsync: Boolean = false
)
