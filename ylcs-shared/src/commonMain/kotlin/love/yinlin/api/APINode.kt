package love.yinlin.api

import kotlinx.serialization.Serializable

object APICode {
    const val SUCCESS = 0
    const val FORBIDDEN = 1
    const val UNAUTHORIZED = 2
    const val FAILED = 3
}

sealed interface APIMethod {
    data object None : APIMethod
    data object Get : APIMethod
    data object Post : APIMethod
    data object Form : APIMethod
}

typealias APIFile = String
typealias APIFiles = List<APIFile>

object Request {
    @Serializable
    data object Default
}

object Response {
    @Serializable
    data object Default
}

@Serializable
data object NoFiles

abstract class APIPath<Request : Any, Response : Any, Files: Any, Method : APIMethod> protected constructor(
    private val path: String
) {
    override fun toString(): String = path
}

abstract class APINode protected constructor(
    parent: APINode?,
    name: String
) : APIPath<Request.Default, Response.Default, NoFiles, APIMethod.None>(if (parent != null) "$parent/$name" else "")

abstract class APIRoute<Request : Any, Response : Any, Files: Any, Method : APIMethod> protected constructor(
    parent: APINode,
    name: String
) : APIPath<Request, Response, Files, Method>("$parent/$name")

typealias APIGet<Request, Response> = APIRoute<Request, Response, NoFiles, APIMethod.Get>
typealias APIPost<Request, Response> = APIRoute<Request, Response, NoFiles, APIMethod.Post>
typealias APIForm<Request, Response, Files> = APIRoute<Request, Response, Files, APIMethod.Form>
typealias APIRequest<Request, Files, Method> = APIRoute<Request, Response.Default, Files, Method>
typealias APIResponse<Response, Files, Method> = APIRoute<Request.Default, Response, Files, Method>
typealias APIGetRequest<Request> = APIRequest<Request, NoFiles, APIMethod.Get>
typealias APIPostRequest<Request> = APIRequest<Request, NoFiles, APIMethod.Post>
typealias APIFormRequest<Request, Files> = APIRequest<Request, Files, APIMethod.Form>
typealias APIGetResponse<Response> = APIResponse<Response, NoFiles, APIMethod.Get>
typealias APIPostResponse<Response> = APIResponse<Response, NoFiles, APIMethod.Post>
typealias APIFormResponse<Response, Files> = APIResponse<Response, Files, APIMethod.Form>