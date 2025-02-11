package eu.vendeli.tgbot.api.media

import eu.vendeli.tgbot.interfaces.MediaAction
import eu.vendeli.tgbot.interfaces.features.*
import eu.vendeli.tgbot.types.Message
import eu.vendeli.tgbot.types.internal.ImplicitFile
import eu.vendeli.tgbot.types.internal.MediaContentType
import eu.vendeli.tgbot.types.internal.TgMethod
import eu.vendeli.tgbot.types.internal.options.AudioOptions

class SendAudioAction(private val audio: ImplicitFile<*>) :
    MediaAction<Message>,
    OptionAble,
    MarkupAble,
    CaptionAble,
    OptionsFeature<SendAudioAction, AudioOptions>,
    MarkupFeature<SendAudioAction>,
    CaptionFeature<SendAudioAction> {
    override val method: TgMethod = TgMethod("sendAudio")
    override var options = AudioOptions()
    override val parameters: MutableMap<String, Any?> = mutableMapOf()

    override val MediaAction<Message>.defaultType: MediaContentType
        get() = MediaContentType.Audio
    override val MediaAction<Message>.media: ImplicitFile<*>
        get() = audio
    override val MediaAction<Message>.dataField: String
        get() = "audio"

}

fun audio(block: () -> String) = SendAudioAction(ImplicitFile.FileId(block()))

fun audio(ba: ByteArray) = SendAudioAction(ImplicitFile.InputFile(ba))
