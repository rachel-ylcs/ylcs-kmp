package love.yinlin.cs

import kotlin.jvm.JvmName
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface API<T : APIType> {
    val route: String

    companion object {
        val post = APIChainPost()
        val form = APIChainForm()
    }
}

interface API00<T : APIType> : API<T>
interface API10<T : APIType, I1> : API<T>
interface API20<T : APIType, I1, I2> : API<T>
interface API30<T : APIType, I1, I2, I3> : API<T>
interface API40<T : APIType, I1, I2, I3, I4> : API<T>
interface API50<T : APIType, I1, I2, I3, I4, I5> : API<T>
interface API01<T : APIType, O1> : API<T>
interface API11<T : APIType, I1, O1> : API<T>
interface API21<T : APIType, I1, I2, O1> : API<T>
interface API31<T : APIType, I1, I2, I3, O1> : API<T>
interface API41<T : APIType, I1, I2, I3, I4, O1> : API<T>
interface API51<T : APIType, I1, I2, I3, I4, I5, O1> : API<T>
interface API02<T : APIType, O1, O2> : API<T>
interface API12<T : APIType, I1, O1, O2> : API<T>
interface API22<T : APIType, I1, I2, O1, O2> : API<T>
interface API32<T : APIType, I1, I2, I3, O1, O2> : API<T>
interface API42<T : APIType, I1, I2, I3, I4, O1, O2> : API<T>
interface API52<T : APIType, I1, I2, I3, I4, I5, O1, O2> : API<T>
interface API03<T : APIType, O1, O2, O3> : API<T>
interface API13<T : APIType, I1, O1, O2, O3> : API<T>
interface API23<T : APIType, I1, I2, O1, O2, O3> : API<T>
interface API33<T : APIType, I1, I2, I3, O1, O2, O3> : API<T>
interface API43<T : APIType, I1, I2, I3, I4, O1, O2, O3> : API<T>
interface API53<T : APIType, I1, I2, I3, I4, I5, O1, O2, O3> : API<T>
interface API04<T : APIType, O1, O2, O3, O4> : API<T>
interface API14<T : APIType, I1, O1, O2, O3, O4> : API<T>
interface API24<T : APIType, I1, I2, O1, O2, O3, O4> : API<T>
interface API34<T : APIType, I1, I2, I3, O1, O2, O3, O4> : API<T>
interface API44<T : APIType, I1, I2, I3, I4, O1, O2, O3, O4> : API<T>
interface API54<T : APIType, I1, I2, I3, I4, I5, O1, O2, O3, O4> : API<T>
interface API05<T : APIType, O1, O2, O3, O4, O5> : API<T>
interface API15<T : APIType, I1, O1, O2, O3, O4, O5> : API<T>
interface API25<T : APIType, I1, I2, O1, O2, O3, O4, O5> : API<T>
interface API35<T : APIType, I1, I2, I3, O1, O2, O3, O4, O5> : API<T>
interface API45<T : APIType, I1, I2, I3, I4, O1, O2, O3, O4, O5> : API<T>
interface API55<T : APIType, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5> : API<T>

class APIChainPost internal constructor() {
    @JvmName("i0")
    fun i() = APIChain0<APIType.Post>()
    @JvmName("i1")
    fun <I1> i() = APIChain1<APIType.Post, I1>()
    @JvmName("i2")
    fun <I1, I2> i() = APIChain2<APIType.Post, I1, I2>()
    @JvmName("i3")
    fun <I1, I2, I3> i() = APIChain3<APIType.Post, I1, I2, I3>()
    @JvmName("i4")
    fun <I1, I2, I3, I4> i() = APIChain4<APIType.Post, I1, I2, I3, I4>()
    @JvmName("i5")
    fun <I1, I2, I3, I4, I5> i() = APIChain5<APIType.Post, I1, I2, I3, I4, I5>()
}

class APIChainForm internal constructor() {
    @JvmName("i1")
    fun <I1> i() = APIChain1<APIType.Form, I1>()
    @JvmName("i2")
    fun <I1, I2> i() = APIChain2<APIType.Form, I1, I2>()
    @JvmName("i3")
    fun <I1, I2, I3> i() = APIChain3<APIType.Form, I1, I2, I3>()
    @JvmName("i4")
    fun <I1, I2, I3, I4> i() = APIChain4<APIType.Form, I1, I2, I3, I4>()
    @JvmName("i5")
    fun <I1, I2, I3, I4, I5> i() = APIChain5<APIType.Form, I1, I2, I3, I4, I5>()
}

class APIChain0<T : APIType> internal constructor() {
    @JvmName("o0")
    fun o() = APIDelegate { object : API00<T> { override val route: String = it } }
    @JvmName("o1")
    fun <O1> o() = APIDelegate { object : API01<T, O1> { override val route: String = it } }
    @JvmName("o2")
    fun <O1, O2> o() = APIDelegate { object : API02<T, O1, O2> { override val route: String = it } }
    @JvmName("o3")
    fun <O1, O2, O3> o() = APIDelegate { object : API03<T, O1, O2, O3> { override val route: String = it } }
    @JvmName("o4")
    fun <O1, O2, O3, O4> o() = APIDelegate { object : API04<T, O1, O2, O3, O4> { override val route: String = it } }
    @JvmName("o5")
    fun <O1, O2, O3, O4, O5> o() = APIDelegate { object : API05<T, O1, O2, O3, O4, O5> { override val route: String = it } }
}

class APIChain1<T : APIType, I1> internal constructor() {
    @JvmName("o0")
    fun o() = APIDelegate { object : API10<T, I1> { override val route: String = it } }
    @JvmName("o1")
    fun <O1> o() = APIDelegate { object : API11<T, I1, O1> { override val route: String = it } }
    @JvmName("o2")
    fun <O1, O2> o() = APIDelegate { object : API12<T, I1, O1, O2> { override val route: String = it } }
    @JvmName("o3")
    fun <O1, O2, O3> o() = APIDelegate { object : API13<T, I1, O1, O2, O3> { override val route: String = it } }
    @JvmName("o4")
    fun <O1, O2, O3, O4> o() = APIDelegate { object : API14<T, I1, O1, O2, O3, O4> { override val route: String = it } }
    @JvmName("o5")
    fun <O1, O2, O3, O4, O5> o() = APIDelegate { object : API15<T, I1, O1, O2, O3, O4, O5> { override val route: String = it } }
}

class APIChain2<T : APIType, I1, I2> internal constructor() {
    @JvmName("o0")
    fun o() = APIDelegate { object : API20<T, I1, I2> { override val route: String = it } }
    @JvmName("o1")
    fun <O1> o() = APIDelegate { object : API21<T, I1, I2, O1> { override val route: String = it } }
    @JvmName("o2")
    fun <O1, O2> o() = APIDelegate { object : API22<T, I1, I2, O1, O2> { override val route: String = it } }
    @JvmName("o3")
    fun <O1, O2, O3> o() = APIDelegate { object : API23<T, I1, I2, O1, O2, O3> { override val route: String = it } }
    @JvmName("o4")
    fun <O1, O2, O3, O4> o() = APIDelegate { object : API24<T, I1, I2, O1, O2, O3, O4> { override val route: String = it } }
    @JvmName("o5")
    fun <O1, O2, O3, O4, O5> o() = APIDelegate { object : API25<T, I1, I2, O1, O2, O3, O4, O5> { override val route: String = it } }
}

class APIChain3<T : APIType, I1, I2, I3> internal constructor() {
    @JvmName("o0")
    fun o() = APIDelegate { object : API30<T, I1, I2, I3> { override val route: String = it } }
    @JvmName("o1")
    fun <O1> o() = APIDelegate { object : API31<T, I1, I2, I3, O1> { override val route: String = it } }
    @JvmName("o2")
    fun <O1, O2> o() = APIDelegate { object : API32<T, I1, I2, I3, O1, O2> { override val route: String = it } }
    @JvmName("o3")
    fun <O1, O2, O3> o() = APIDelegate { object : API33<T, I1, I2, I3, O1, O2, O3> { override val route: String = it } }
    @JvmName("o4")
    fun <O1, O2, O3, O4> o() = APIDelegate { object : API34<T, I1, I2, I3, O1, O2, O3, O4> { override val route: String = it } }
    @JvmName("o5")
    fun <O1, O2, O3, O4, O5> o() = APIDelegate { object : API35<T, I1, I2, I3, O1, O2, O3, O4, O5> { override val route: String = it } }
}

class APIChain4<T : APIType, I1, I2, I3, I4> internal constructor() {
    @JvmName("o0")
    fun o() = APIDelegate { object : API40<T, I1, I2, I3, I4> { override val route: String = it } }
    @JvmName("o1")
    fun <O1> o() = APIDelegate { object : API41<T, I1, I2, I3, I4, O1> { override val route: String = it } }
    @JvmName("o2")
    fun <O1, O2> o() = APIDelegate { object : API42<T, I1, I2, I3, I4, O1, O2> { override val route: String = it } }
    @JvmName("o3")
    fun <O1, O2, O3> o() = APIDelegate { object : API43<T, I1, I2, I3, I4, O1, O2, O3> { override val route: String = it } }
    @JvmName("o4")
    fun <O1, O2, O3, O4> o() = APIDelegate { object : API44<T, I1, I2, I3, I4, O1, O2, O3, O4> { override val route: String = it } }
    @JvmName("o5")
    fun <O1, O2, O3, O4, O5> o() = APIDelegate { object : API45<T, I1, I2, I3, I4, O1, O2, O3, O4, O5> { override val route: String = it } }
}

class APIChain5<T : APIType, I1, I2, I3, I4, I5> internal constructor() {
    @JvmName("o0")
    fun o() = APIDelegate { object : API50<T, I1, I2, I3, I4, I5> { override val route: String = it } }
    @JvmName("o1")
    fun <O1> o() = APIDelegate { object : API51<T, I1, I2, I3, I4, I5, O1> { override val route: String = it } }
    @JvmName("o2")
    fun <O1, O2> o() = APIDelegate { object : API52<T, I1, I2, I3, I4, I5, O1, O2> { override val route: String = it } }
    @JvmName("o3")
    fun <O1, O2, O3> o() = APIDelegate { object : API53<T, I1, I2, I3, I4, I5, O1, O2, O3> { override val route: String = it } }
    @JvmName("o4")
    fun <O1, O2, O3, O4> o() = APIDelegate { object : API54<T, I1, I2, I3, I4, I5, O1, O2, O3, O4> { override val route: String = it } }
    @JvmName("o5")
    fun <O1, O2, O3, O4, O5> o() = APIDelegate { object : API55<T, I1, I2, I3, I4, I5, O1, O2, O3, O4, O5> { override val route: String = it } }
}

class APIDelegate<A : API<out APIType>> internal constructor(private val factory: (String) -> A) : ReadOnlyProperty<Any?, A> {
    private lateinit var api: A

    private fun parseAPIRoute(name: String): String = name.replace("Api([A-Z])([a-z]*)([A-Z].*)".toRegex()) {
        val (_, v1, v2, v3) = it.groupValues
        "/${v1.lowercase()}$v2/${v3.replaceFirstChar { c -> c.lowercase() }}"
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): A {
        if (!::api.isInitialized) api = factory(parseAPIRoute(property.name))
        return api
    }
}