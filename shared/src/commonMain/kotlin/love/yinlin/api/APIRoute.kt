package love.yinlin.api

import kotlinx.serialization.Serializable

object APICode {
	const val SUCCESS = 0
	const val FORBIDDEN = 1
	const val UNAUTHORIZED = 2
	const val FAILED = 3
}

enum class APIMethod {
	Get, Post, Form
}

object Default {
	@Serializable
	data object Request

	@Serializable
	data object Response
}

abstract class APIPath<Request : Any, Response : Any> protected constructor(
	val path: String,
	val method: APIMethod
)

abstract class APINode protected constructor(
	parent: APINode?,
	name: String
) : APIPath<Default.Request, Default.Response>(if (parent != null) "${parent.path}/$name" else "", APIMethod.Post)

abstract class APIRoute<Request : Any, Response : Any> protected constructor(
	parent: APINode,
	name: String,
	method: APIMethod = APIMethod.Post
) : APIPath<Request, Response>("${parent.path}/$name", method)

typealias APIRequest<Request> = APIRoute<Request, Default.Response>
typealias APIResponse<Response> = APIRoute<Default.Request, Response>