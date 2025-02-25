package love.yinlin.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.rachel.ActivityDetails
import love.yinlin.data.rachel.ActivityInfo
import love.yinlin.data.rachel.Comment
import love.yinlin.data.rachel.SubComment
import love.yinlin.data.rachel.TopicDetails
import love.yinlin.data.rachel.UserProfile
import love.yinlin.data.rachel.UserPublicProfile

// ------------  API 配置  ------------

object APIConfig {
	const val DEBUG = true
	val URL = if (DEBUG) "http://localhost:1211" else "https://yinlin.love"

	const val MIN_PAGE_NUM = 10
	const val MAX_PAGE_NUM = 20
	val Int.coercePageNum: Int get() = this.coerceAtLeast(MIN_PAGE_NUM).coerceAtMost(MAX_PAGE_NUM)
}

// ------------  客户端资源  ------------

@Serializable(with = ClientFile.ClientFileSerializer::class)
data class ClientFile(val path: String) {
	object ClientFileSerializer : KSerializer<ClientFile> {
		override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.ClientFile", PrimitiveKind.STRING)
		override fun serialize(encoder: Encoder, value: ClientFile) = encoder.encodeString(value.path)
		override fun deserialize(decoder: Decoder) = ClientFile(decoder.decodeString())
	}

	override fun toString(): String = path
}

// ------------  服务端资源  ------------

open class ResNode protected constructor(val path: String) {
	constructor(parent: ResNode, name: String) : this("${parent.path}/$name")
}

object ServerRes : ResNode("public") {
	object Activity : ResNode(this, "activity") {
		fun activity(uniqueId: String) = ResNode(this, "${uniqueId}.webp")
	}
	object Assets : ResNode(this, "assets") {
		val DefaultAvatar = ResNode(this, "default_avatar.webp")
		val DefaultWall = ResNode(this, "default_wall.webp")
	}
	object Users : ResNode(this, "users") {
		class User(uid: Int) : ResNode(this, "$uid") {
			val avatar = ResNode(this, "avatar.webp")
			val wall = ResNode(this, "wall.webp")
			inner class Pics : ResNode(this, "pics") {
				fun pic(uniqueId: String) = ResNode(this, "${uniqueId}.webp")
			}
		}
	}
	val Photo = ResNode(this, "photo.json")
	val Server = ResNode(this, "server.json")
}

// ------------  API 接口  ------------

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


	@Serializable
	data class AuthorizationRequest(val name: String, val pwd: String)

	@Serializable
	data class PageAscRequest(val offset: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)

	@Serializable
	data class PageDescRequest(val offset: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
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

object API : APINode(null, "") {
	object User : APINode(this, "user") {
		object Account : APINode(this, "account") {
			object Login : APIRoute<Default.AuthorizationRequest, String>(this, "login")

			object Logoff : APIRequest<String>(this, "logoff")

			object UpdateToken : APIRoute<String, String>(this, "updateToken")

			object Register : APIRequest<Register.Request>(this, "register") {
				@Serializable
				data class Request(val name: String, val pwd: String, val inviterName: String)
			}

			object ForgotPassword : APIRequest<Default.AuthorizationRequest>(this, "forgotPassword")
		}

		object Activity : APINode(this, "activity") {
			object GetActivities : APIResponse<List<love.yinlin.data.rachel.Activity>>(this, "getActivities")

			object GetActivityDetails : APIRoute<String, ActivityDetails>(this, "getActivityDetails")

			object AddActivity : APIRequest<AddActivity.Request>(this, "addActivity", APIMethod.Form) {
				@Serializable
				data class Request(val token: String, val info: ActivityInfo, val pics: List<ClientFile>)
			}

			object ModifyActivity : APIRequest<ModifyActivity.Request>(this, "modifyActivity", APIMethod.Form) {
				@Serializable
				data class Request(val token: String, val info: ActivityInfo, val pics: List<ClientFile>?)
			}

			object DeleteActivity : APIRequest<DeleteActivity.Request>(this, "deleteActivity") {
				@Serializable
				data class Request(val token: String, val ts: String)
			}
		}

		object Backup : APINode(this, "backup") {
			object UploadPlaylist : APIRequest<UploadPlaylist.Request>(this, "uploadPlaylist") {
				@Serializable
				data class Request(val token: String, val playlist: JsonObject)
			}

			object DownloadPlaylist : APIRoute<String, JsonObject>(this, "downloadPlaylist")
		}

		object Info : APINode(this, "info") {
			object SendFeedback : APIRequest<SendFeedback.Request>(this, "sendFeedback") {
				@Serializable
				data class Request(val token: String, val content: String)
			}
		}

		object Mail : APINode(this, "mail") {
			object GetMails : APIRoute<GetMails.Request, List<love.yinlin.data.rachel.Mail>>(this, "getMails") {
				@Serializable
				data class Request(val token: String, val offset: Long = Long.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object ProcessMail : APIRequest<ProcessMail.Request>(this, "processMail") {
				@Serializable
				data class Request(val token: String, val mid: Int, val confirm: Boolean)
			}

			object DeleteMail : APIRequest<DeleteMail.Request>(this, "deleteMail") {
				@Serializable
				data class Request(val token: String, val mid: Int)
			}
		}

		object Profile : APINode(this, "profile") {
			object GetProfile : APIRoute<String, UserProfile>(this, "getProfile")

			object GetPublicProfile : APIRoute<Int, UserPublicProfile>(this, "getPublicProfile")

			object UpdateName : APIRequest<UpdateName.Request>(this, "updateName") {
				@Serializable
				data class Request(val token: String, val name: String)
			}

			object UpdateAvatar : APIRequest<UpdateAvatar.Request>(this, "updateAvatar", APIMethod.Form) {
				@Serializable
				data class Request(val token: String, val avatar: ClientFile)
			}

			object UpdateSignature : APIRequest<UpdateSignature.Request>(this, "updateSignature") {
				@Serializable
				data class Request(val token: String, val signature: String)
			}

			object UpdateWall : APIRequest<UpdateWall.Request>(this, "updateWall", APIMethod.Form) {
				@Serializable
				data class Request(val token: String, val wall: ClientFile)
			}

			object Signin : APIRequest<String>(this, "signin")
		}

		object Topic : APINode(this, "topic") {
			object GetTopics : APIRoute<GetTopics.Request, List<love.yinlin.data.rachel.Topic>>(this, "getTopics") {
				@Serializable
				data class Request(val uid: Int, val isTop: Boolean = true, val offset: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetLatestTopics : APIRoute<Default.PageDescRequest, List<love.yinlin.data.rachel.Topic>>(this, "getLatestTopics")

			object GetHotTopics : APIRoute<Default.PageDescRequest, List<love.yinlin.data.rachel.Topic>>(this, "getHotTopics")

			object GetSectionTopics : APIRoute<GetSectionTopics.Request, List<love.yinlin.data.rachel.Topic>>(this, "getSectionTopics") {
				@Serializable
				data class Request(val section: Int, val offset: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetTopicDetails : APIRoute<Int, TopicDetails>(this, "getTopicDetails")

			object GetTopicComments : APIRoute<GetTopicComments.Request, List<Comment>>(this, "getTopicComments") {
				@Serializable
				data class Request(val tid: Int, val rawSection: Int, val isTop: Boolean = true, val offset: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetTopicSubComments : APIRoute<GetTopicSubComments.Request, List<SubComment>>(this, "getTopicSubComments") {
				@Serializable
				data class Request(val cid: Int, val rawSection: Int, val offset: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object SendTopic : APIRoute<SendTopic.Request, SendTopic.Response>(this, "sendTopic", APIMethod.Form) {
				@Serializable
				data class Request(val token: String, val title: String, val content: String, val section: Int, val pics: List<ClientFile>)
				@Serializable
				data class Response(val tid: Int, val pic: String?)
			}

			object UpdateTopicTop : APIRequest<UpdateTopicTop.Request>(this, "updateTopicTop") {
				@Serializable
				data class Request(val token: String, val tid: Int, val isTop: Boolean)
			}

			object DeleteTopic : APIRequest<DeleteTopic.Request>(this, "deleteTopic") {
				@Serializable
				data class Request(val token: String, val tid: Int)
			}

			object MoveTopic : APIRequest<MoveTopic.Request>(this, "moveTopic") {
				@Serializable
				data class Request(val token: String, val tid: Int, val section: Int)
			}

			object SendCoin : APIRequest<SendCoin.Request>(this, "sendCoin") {
				@Serializable
				data class Request(val token: String, val uid: Int, val tid: Int, val value: Int)
			}

			object SendComment : APIRoute<SendComment.Request, Int>(this, "sendComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val rawSection: Int, val content: String)
			}

			object SendSubComment : APIRoute<SendSubComment.Request, Int>(this, "sendSubComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val cid: Int, val rawSection: Int, val content: String)
			}

			object UpdateCommentTop : APIRequest<UpdateCommentTop.Request>(this, "updateCommentTop") {
				@Serializable
				data class Request(val token: String, val tid: Int, val cid: Int, val rawSection: Int, val isTop: Boolean)
			}

			object DeleteComment : APIRequest<DeleteComment.Request>(this, "deleteComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val cid: Int, val rawSection: Int)
			}

			object DeleteSubComment : APIRequest<DeleteSubComment.Request>(this, "deleteSubComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val pid: Int, val cid: Int, val rawSection: Int)
			}
		}
	}

	object Test : APINode(this, "test") {
		object Get : APIRequest<String>(this, "get", APIMethod.Get)

		object Post : APIRequest<Post.Request>(this, "post", APIMethod.Form) {
			@Serializable
			data class Request(val avatar: ClientFile, val pics: List<ClientFile>)
		}
	}
}