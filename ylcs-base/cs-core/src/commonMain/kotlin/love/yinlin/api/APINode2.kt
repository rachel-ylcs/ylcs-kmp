package love.yinlin.api

import kotlinx.serialization.Serializable

// TODO: 迁移升级

data object APICode2 {
    const val SUCCESS = 0
    const val FORBIDDEN = 1
    const val UNAUTHORIZED = 2
    const val FAILED = 3
}

sealed interface APIMethod2 {
    data object None : APIMethod2
    data object Get : APIMethod2
    data object Post : APIMethod2
    data object Form : APIMethod2
}

abstract class APIPath2<Request : Any, Response : Any, Files: Any, Method : APIMethod2> protected constructor(
    private val path: String
) {
    override fun toString(): String = path
}

abstract class APINode2 protected constructor(
    parent: APINode2?,
    name: String
) : APIPath2<Request.Default, Response.Default, NoFiles, APIMethod2.None>(if (parent != null) "$parent/$name" else "")

abstract class APIRoute2<Request : Any, Response : Any, Files: Any, Method : APIMethod2> protected constructor(
    parent: APINode2,
    name: String
) : APIPath2<Request, Response, Files, Method>("$parent/$name")

typealias APIFile2 = String
typealias APIFiles2 = List<APIFile2>

typealias APIGet2<Request, Response> = APIRoute2<Request, Response, NoFiles, APIMethod2.Get>
typealias APIPost2<Request, Response> = APIRoute2<Request, Response, NoFiles, APIMethod2.Post>
typealias APIForm2<Request, Response, Files> = APIRoute2<Request, Response, Files, APIMethod2.Form>
typealias APIRequest2<Request, Files, Method> = APIRoute2<Request, Response.Default, Files, Method>
typealias APIResponse2<Response, Files, Method> = APIRoute2<Request.Default, Response, Files, Method>
typealias APIGetRequest2<Request> = APIRequest2<Request, NoFiles, APIMethod2.Get>
typealias APIPostRequest2<Request> = APIRequest2<Request, NoFiles, APIMethod2.Post>
typealias APIFormRequest2<Request, Files> = APIRequest2<Request, Files, APIMethod2.Form>
typealias APIGetResponse2<Response> = APIResponse2<Response, NoFiles, APIMethod2.Get>
typealias APIPostResponse2<Response> = APIResponse2<Response, NoFiles, APIMethod2.Post>
typealias APIFormResponse2<Response, Files> = APIResponse2<Response, Files, APIMethod2.Form>

@Serializable
data object NoFiles

object Request {
    @Serializable
    data object Default
}

object Response {
    @Serializable
    data object Default
}

open class ResNode2 protected constructor(val path: String) {
    constructor(parent: ResNode2, name: String) : this("${parent.path}/$name")

    override fun toString(): String = path
}