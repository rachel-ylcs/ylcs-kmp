package love.yinlin.uri

import android.content.Context
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

open class ContentUri(private val context: Context, override val path: String) : ImplicitUri {
    override val source: Source get() = context.contentResolver.openInputStream(Uri.parse(path)!!.toAndroidUri())!!.asSource().buffered()
    override val sink: Sink get() = context.contentResolver.openOutputStream(Uri.parse(path)!!.toAndroidUri())!!.asSink().buffered()
}