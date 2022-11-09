package eu.vendeli

import eu.vendeli.tgbot.annotations.CommandHandler
import eu.vendeli.tgbot.annotations.InputHandler
import eu.vendeli.tgbot.annotations.UnprocessedHandler
import eu.vendeli.tgbot.enums.MethodPriority

class TgAnnotationsModel {

    @CommandHandler(["test"])
    fun test() {
    }

    @InputHandler(["testInp"])
    fun test2() {
    }

    @UnprocessedHandler
    fun test3() {
    }
}

@CommandHandler(["test2"])
suspend fun testMethod() {
}

@InputHandler(["testInp2"])
suspend fun testMethod2() {
}

object TestObj {
    @CommandHandler(["test3"])
    fun test() {
    }

    @InputHandler(["testInp3"])
    fun test2() {
    }
}

class tgUnprocessedHandleModel {

    @UnprocessedHandler(priority = MethodPriority.LOW)
    fun test1() {
    }

    @UnprocessedHandler(priority = MethodPriority.HIGHEST)
    fun test2() {
    }

}

@UnprocessedHandler
fun onTest() {
}