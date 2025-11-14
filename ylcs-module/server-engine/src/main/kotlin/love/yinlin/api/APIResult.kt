package love.yinlin.api

@ConsistentCopyVisibility
data class APIResult1<O1> internal constructor(val o1: O1)
@ConsistentCopyVisibility
data class APIResult2<O1, O2> internal constructor(val o1: O1, val o2: O2)
@ConsistentCopyVisibility
data class APIResult3<O1, O2, O3> internal constructor(val o1: O1, val o2: O2, val o3: O3)
@ConsistentCopyVisibility
data class APIResult4<O1, O2, O3, O4> internal constructor(val o1: O1, val o2: O2, val o3: O3, val o4: O4)
@ConsistentCopyVisibility
data class APIResult5<O1, O2, O3, O4, O5> internal constructor(val o1: O1, val o2: O2, val o3: O3, val o4: O4, val o5: O5)

open class APIResponseScope {
    fun expire(): Nothing = throw UnauthorizedException(null)
    fun failure(message: String? = null): Nothing = throw FailureException(message)
}

class APIResultScope1<O1> : APIResponseScope() {
    fun result(o1: O1) = APIResult1(o1)
}

class APIResultScope2<O1, O2> : APIResponseScope() {
    fun result(o1: O1, o2: O2) = APIResult2(o1, o2)
}

class APIResultScope3<O1, O2, O3> : APIResponseScope() {
    fun result(o1: O1, o2: O2, o3: O3) = APIResult3(o1, o2, o3)
}

class APIResultScope4<O1, O2, O3, O4> : APIResponseScope() {
    fun result(o1: O1, o2: O2, o3: O3, o4: O4) = APIResult4(o1, o2, o3, o4)
}

class APIResultScope5<O1, O2, O3, O4, O5> : APIResponseScope() {
    fun result(o1: O1, o2: O2, o3: O3, o4: O4, o5: O5) = APIResult5(o1, o2, o3, o4, o5)
}