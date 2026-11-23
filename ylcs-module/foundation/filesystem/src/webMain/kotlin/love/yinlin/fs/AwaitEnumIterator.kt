package love.yinlin.fs

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.Promise
import kotlin.js.js

@OptIn(ExperimentalWasmJsInterop::class)
fun <R : JsAny?> awaitEnumIterator(value: JsAny): Promise<JsArray<R>> = js("""
    (function(items) {
        const iterator = (items[Symbol.asyncIterator] || items[Symbol.iterator]).call(items);
        const entries = [];
        function next() {
            return Promise.resolve(iterator.next()).then(function(result) {
                if (result.done) return entries;
                entries.push(result.value);
                return next();
            });
        }
        return next();
    })(value)
""")
