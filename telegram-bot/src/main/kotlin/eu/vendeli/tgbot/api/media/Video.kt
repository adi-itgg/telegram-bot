package eu.vendeli.tgbot.api.media

import eu.vendeli.tgbot.interfaces.MediaAction
import eu.vendeli.tgbot.interfaces.features.*
import eu.vendeli.tgbot.types.Message
import eu.vendeli.tgbot.types.internal.ImplicitFile
import eu.vendeli.tgbot.types.internal.MediaContentType
import eu.vendeli.tgbot.types.internal.TgMethod
import eu.vendeli.tgbot.types.internal.options.VideoOptions

class SendVideoAction(private val video: ImplicitFile<*>) :
    MediaAction<Message>,
    OptionAble,
    MarkupAble,
    CaptionAble,
    OptionsFeature<SendVideoAction, VideoOptions>,
    MarkupFeature<SendVideoAction>,
    CaptionFeature<SendVideoAction> {
    override val method: TgMethod = TgMethod("sendVideo")
    override var options = VideoOptions()
    override val parameters: MutableMap<String, Any?> = mutableMapOf()

    override val MediaAction<Message>.defaultType: MediaContentType
        get() = MediaContentType.VideoMp4
    override val MediaAction<Message>.media: ImplicitFile<*>
        get() = video
    override val MediaAction<Message>.dataField: String
        get() = "video"
}

fun video(block: () -> String) = SendVideoAction(ImplicitFile.FileId(block()))

fun video(ba: ByteArray) = SendVideoAction(ImplicitFile.InputFile(ba))
