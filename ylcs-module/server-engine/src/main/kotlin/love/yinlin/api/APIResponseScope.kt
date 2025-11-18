package love.yinlin.api

open class APIResponseScope : APICallbackScope()

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