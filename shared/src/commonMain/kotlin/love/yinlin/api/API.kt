package love.yinlin.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.rachel.Comment
import love.yinlin.data.rachel.SubComment
import love.yinlin.data.rachel.TopicDetails
import love.yinlin.data.rachel.UserProfile
import love.yinlin.data.rachel.UserPublicProfile
import love.yinlin.platform.Platform

// ------------  API 配置  ------------

object APIConfig {
	const val MIN_PAGE_NUM = 20
	const val MAX_PAGE_NUM = 30
	val Int.coercePageNum: Int get() = this.coerceAtLeast(MIN_PAGE_NUM).coerceAtMost(MAX_PAGE_NUM)
}

// ------------  客户端资源  ------------

@Serializable(with = APIFile.APIFileSerializer::class)
data class APIFile(private val path: String) {
	object APIFileSerializer : KSerializer<APIFile> {
		override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("json.convert.APIFile", PrimitiveKind.STRING)
		override fun serialize(encoder: Encoder, value: APIFile) = encoder.encodeString(value.path)
		override fun deserialize(decoder: Decoder) = APIFile(decoder.decodeString())
	}

	override fun toString(): String = path
}

typealias APIFiles = List<APIFile>

// ------------  服务端资源  ------------

open class ResNode protected constructor(val path: String) {
	constructor(parent: ResNode, name: String) : this("${parent.path}/$name")

	override fun toString(): String = path
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

	val Server = ResNode(this, "server.json")
	val Update = ResNode(this, "update.json")
	val Photo = ResNode(this, "photo.json")
}

// ------------  API 接口  ------------

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

object API : APINode(null, "") {
	object User : APINode(this, "user") {
		object Account : APINode(this, "account") {
			object Login : APIPost<Login.Request, String>(this, "login") {
				@Serializable
				data class Request(val name: String, val pwd: String, val platform: Platform)
			}

			object Logoff : APIPostRequest<String>(this, "logoff")

			object UpdateToken : APIPost<String, String>(this, "updateToken")

			object Register : APIPostRequest<Register.Request>(this, "register") {
				@Serializable
				data class Request(val name: String, val pwd: String, val inviterName: String)
			}

			object ForgotPassword : APIPostRequest<ForgotPassword.Request>(this, "forgotPassword") {
				@Serializable
				data class Request(val name: String, val pwd: String)
			}
		}

		object Activity : APINode(this, "activity") {
			object GetActivities : APIPostResponse<List<love.yinlin.data.rachel.Activity>>(this, "getActivities")

			object AddActivity : APIForm<AddActivity.Request, AddActivity.Response, AddActivity.Files>(this, "addActivity") {
				@Serializable
				data class Request(val token: String, val activity: love.yinlin.data.rachel.Activity)
				@Serializable
				data class Files(val pic: APIFile?, val pics: APIFiles?)
				@Serializable
				data class Response(val aid: Int, val pic: String?, val pics: List<String>)
			}

			object ModifyActivityInfo : APIPostRequest<ModifyActivityInfo.Request>(this, "modifyActivityInfo") {
				@Serializable
				data class Request(val token: String, val activity: love.yinlin.data.rachel.Activity)
			}

			object ModifyActivityPicture : APIFormRequest<ModifyActivityPicture.Request, ModifyActivityPicture.Files>(this, "modifyActivityPicture") {
				@Serializable
				data class Request(val token: String, val aid: Int)
				@Serializable
				data class Files(val pic: APIFile)
			}

			object DeleteActivityPicture : APIPostRequest<DeleteActivityPicture.Request>(this, "deleteActivityPicture") {
				@Serializable
				data class Request(val token: String, val aid: Int)
			}

			object AddActivityPictures : APIFormRequest<AddActivityPictures.Request, AddActivityPictures.Files>(this, "addActivityPictures") {
				@Serializable
				data class Request(val token: String, val aid: Int)
				@Serializable
				data class Files(val pic: APIFile)
			}

			object ModifyActivityPictures : APIFormRequest<ModifyActivityPictures.Request, ModifyActivityPictures.Files>(this, "modifyActivityPictures") {
				@Serializable
				data class Request(val token: String, val aid: Int, val index: Int)
				@Serializable
				data class Files(val pic: APIFile)
			}

			object DeleteActivityPictures : APIPostRequest<DeleteActivityPictures.Request>(this, "deleteActivityPictures") {
				@Serializable
				data class Request(val token: String, val aid: Int, val index: Int)
			}

			object DeleteActivity : APIPostRequest<DeleteActivity.Request>(this, "deleteActivity") {
				@Serializable
				data class Request(val token: String, val ts: String)
			}
		}

		object Backup : APINode(this, "backup") {
			object UploadPlaylist : APIPostRequest<UploadPlaylist.Request>(this, "uploadPlaylist") {
				@Serializable
				data class Request(val token: String, val playlist: JsonObject)
			}

			object DownloadPlaylist : APIPost<String, JsonObject>(this, "downloadPlaylist")
		}

		object Info : APINode(this, "info") {
			object SendFeedback : APIPostRequest<SendFeedback.Request>(this, "sendFeedback") {
				@Serializable
				data class Request(val token: String, val content: String)
			}
		}

		object Mail : APINode(this, "mail") {
			object GetMails : APIPost<GetMails.Request, List<love.yinlin.data.rachel.Mail>>(this, "getMails") {
				@Serializable
				data class Request(val token: String, val offset: Long = Long.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object ProcessMail : APIPostRequest<ProcessMail.Request>(this, "processMail") {
				@Serializable
				data class Request(val token: String, val mid: Int, val confirm: Boolean)
			}

			object DeleteMail : APIPostRequest<DeleteMail.Request>(this, "deleteMail") {
				@Serializable
				data class Request(val token: String, val mid: Int)
			}
		}

		object Profile : APINode(this, "profile") {
			object GetProfile : APIPost<String, UserProfile>(this, "getProfile")

			object GetPublicProfile : APIPost<Int, UserPublicProfile>(this, "getPublicProfile")

			object UpdateName : APIPostRequest<UpdateName.Request>(this, "updateName") {
				@Serializable
				data class Request(val token: String, val name: String)
			}

			object UpdateAvatar : APIFormRequest<String, UpdateAvatar.Files>(this, "updateAvatar") {
				@Serializable
				data class Files(val avatar: APIFile)
			}

			object UpdateSignature : APIPostRequest<UpdateSignature.Request>(this, "updateSignature") {
				@Serializable
				data class Request(val token: String, val signature: String)
			}

			object UpdateWall : APIFormRequest<String, UpdateWall.Files>(this, "updateWall") {
				@Serializable
				data class Files(val wall: APIFile)
			}

			object Signin : APIPostRequest<String>(this, "signin")
		}

		object Topic : APINode(this, "topic") {
			object GetTopics : APIPost<GetTopics.Request, List<love.yinlin.data.rachel.Topic>>(this, "getTopics") {
				@Serializable
				data class Request(val uid: Int, val isTop: Boolean = true, val offset: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetLatestTopics : APIPost<GetLatestTopics.Request, List<love.yinlin.data.rachel.Topic>>(this, "getLatestTopics") {
				@Serializable
				data class Request(val offset: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetHotTopics : APIPost<GetHotTopics.Request, List<love.yinlin.data.rachel.Topic>>(this, "getHotTopics") {
				@Serializable
				data class Request(val offset: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetSectionTopics : APIPost<GetSectionTopics.Request, List<love.yinlin.data.rachel.Topic>>(this, "getSectionTopics") {
				@Serializable
				data class Request(val section: Int, val offset: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetTopicDetails : APIPost<Int, TopicDetails>(this, "getTopicDetails")

			object GetTopicComments : APIPost<GetTopicComments.Request, List<Comment>>(this, "getTopicComments") {
				@Serializable
				data class Request(val tid: Int, val rawSection: Int, val isTop: Boolean = true, val offset: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetTopicSubComments : APIPost<GetTopicSubComments.Request, List<SubComment>>(this, "getTopicSubComments") {
				@Serializable
				data class Request(val cid: Int, val rawSection: Int, val offset: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object SendTopic : APIForm<SendTopic.Request, SendTopic.Response, SendTopic.Files>(this, "sendTopic") {
				@Serializable
				data class Request(val token: String, val title: String, val content: String, val section: Int)
				@Serializable
				data class Files(val pics: APIFiles?)
				@Serializable
				data class Response(val tid: Int, val pic: String?)
			}

			object UpdateTopicTop : APIPostRequest<UpdateTopicTop.Request>(this, "updateTopicTop") {
				@Serializable
				data class Request(val token: String, val tid: Int, val isTop: Boolean)
			}

			object DeleteTopic : APIPostRequest<DeleteTopic.Request>(this, "deleteTopic") {
				@Serializable
				data class Request(val token: String, val tid: Int)
			}

			object MoveTopic : APIPostRequest<MoveTopic.Request>(this, "moveTopic") {
				@Serializable
				data class Request(val token: String, val tid: Int, val section: Int)
			}

			object SendCoin : APIPostRequest<SendCoin.Request>(this, "sendCoin") {
				@Serializable
				data class Request(val token: String, val uid: Int, val tid: Int, val value: Int)
			}

			object SendComment : APIPost<SendComment.Request, Int>(this, "sendComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val rawSection: Int, val content: String)
			}

			object SendSubComment : APIPost<SendSubComment.Request, Int>(this, "sendSubComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val cid: Int, val rawSection: Int, val content: String)
			}

			object UpdateCommentTop : APIPostRequest<UpdateCommentTop.Request>(this, "updateCommentTop") {
				@Serializable
				data class Request(val token: String, val tid: Int, val cid: Int, val rawSection: Int, val isTop: Boolean)
			}

			object DeleteComment : APIPostRequest<DeleteComment.Request>(this, "deleteComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val cid: Int, val rawSection: Int)
			}

			object DeleteSubComment : APIPostRequest<DeleteSubComment.Request>(this, "deleteSubComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val pid: Int, val cid: Int, val rawSection: Int)
			}
		}
	}

	object Test : APINode(this, "test") {
		object Get : APIGetRequest<Get.Request>(this, "get") {
			@Serializable
			data class Request(val a: String)
		}

		object Post : APIFormRequest<Post.Request, Post.Files>(this, "post") {
			@Serializable
			data class Request(val a: String, val b: Int)
			@Serializable
			data class Files(val c: APIFile, val d: APIFiles, val e: APIFile?)
		}
	}
}