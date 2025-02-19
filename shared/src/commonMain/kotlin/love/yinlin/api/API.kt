package love.yinlin.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object API : APINode(null, "") {
	object User : APINode(this, "user") {
		object Account : APINode(this, "account") {
			object Login : APIRoute<Login.Request, String>(this, "login") {
				@Serializable
				data class Request(val name: String, val pwd: String)
			}

			object Logoff : APIRequest<String>(this, "logoff")

			object UpdateToken : APIRoute<String, String>(this, "updateToken")

			object Register : APIRequest<Register.Request>(this, "register") {
				@Serializable
				data class Request(val name: String, val pwd: String, val inviterName: String)
			}

			object ForgotPassword : APIRequest<ForgotPassword.Request>(this, "forgotPassword") {
				@Serializable
				data class Request(val name: String, val pwd: String)
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
			object GetMails : APIRoute<GetMails.Request, JsonArray>(this, "getMails") {
				@Serializable
				data class Request(val token: String, val offset: Long = Long.MAX_VALUE, val num: Int = 10)
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
			object GetProfile : APIRoute<String, JsonObject>(this, "getProfile")

			object GetPublicProfile : APIRoute<Int, JsonObject>(this, "getPublicProfile")

			object UpdateName : APIRequest<UpdateName.Request>(this, "updateName") {
				@Serializable
				data class Request(val token: String, val name: String)
			}

			object UpdateAvatar : APIRequest<UpdateAvatar.Request>(this, "updateAvatar", APIMethod.Form) {
				@Serializable
				data class Request(val token: String, val avatar: String)
			}

			object UpdateSignature : APIRequest<UpdateSignature.Request>(this, "updateSignature") {
				@Serializable
				data class Request(val token: String, val signature: String)
			}

			object UpdateWall : APIRequest<UpdateWall.Request>(this, "updateWall", APIMethod.Form) {
				@Serializable
				data class Request(val token: String, val wall: String)
			}

			object Signin : APIRequest<String>(this, "signin")
		}

		object Topic : APINode(this, "topic") {
			object GetTopics : APIRoute<Int, JsonArray>(this, "getTopics")
		}
	}

	object Test : APINode(this, "test") {
		object Get : APIResponse<String>(this, "get", APIMethod.Get)

		object Post : APIResponse<String>(this, "post")
	}
}