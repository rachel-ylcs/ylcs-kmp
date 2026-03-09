package love.yinlin.fs

import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
actual fun <R : JsAny?> awaitEnumIterator(value: JsAny): Promise<JsArray<R>> = js("""
{
    (async function(items) {
        const entries = [];
        for await (const v of items) entries.push(v);
        return entries;
    })(value)
}
""")