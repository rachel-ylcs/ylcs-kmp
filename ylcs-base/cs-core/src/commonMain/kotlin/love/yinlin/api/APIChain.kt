package love.yinlin.api

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

//private fun parseAPIRoute(property: String): String {
//    val (left, right) = property.split('_')
//    val path = left.replace("(?<=.)(?=[A-Z])".toRegex(), "/").lowercase()
//    return "/$path/$right"
//}
//
//class APIChainA(private val apiMethod: APIMethod) {
//    fun i(): APIChainB0 =
//
//    inline fun <reified R1> i(): APIChainB1 {
//
//    }
//
//    inline fun <reified R1, reified R2> i(): APIChainB2 {
//
//    }
//
//    inline fun <reified R1, reified R2, reified R3> i(): APIChainB3 {
//
//    }
//
//    inline fun <reified R1, reified R2, reified R3, reified R4> i(): APIChainB4 {
//
//    }
//
//    inline fun <reified R1, reified R2, reified R3, reified R4, reified R5> i(): APIChainB5 {
//
//    }
//
////    operator fun invoke() = APIChainD { property ->
////        object : APIChain00() {
////            override val method: APIMethod = apiMethod
////            override val route: String = parseAPIRoute(property)
////        }
////    }
//}
//
//class APIChainB0 {
//
//}
//
//class APIChainC() {
//
//}
//
//class APIChainD<A : API>(private val factory: (String) -> A) : ReadOnlyProperty<Any?, A> {
//    private lateinit var api: A
//
//    override fun getValue(thisRef: Any?, property: KProperty<*>): A {
//        if (!::api.isInitialized) api = factory(property.name)
//        return api
//    }
//}
//
//abstract class APIChain00 : API
//
//abstract class APIChain10<R1> : API