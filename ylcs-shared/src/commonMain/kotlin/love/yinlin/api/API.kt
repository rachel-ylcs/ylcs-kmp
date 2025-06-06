package love.yinlin.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.rachel.follows.BlockedUserInfo
import love.yinlin.data.rachel.follows.FollowInfo
import love.yinlin.data.rachel.follows.FollowerInfo
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.data.rachel.song.SongComment
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.SubComment
import love.yinlin.data.rachel.topic.TopicDetails
import love.yinlin.platform.Platform


object API : APINode(null, "") {
	object User : APINode(this, "user") {
		object Account : APINode(this, "account") {
			object GetInviters : APIPostResponse<List<String>>(this, "getInviters")

			object Login : APIPost<Login.Request, String>(this, "login") {
				@Serializable
				data class Request(val name: String, val pwd: String, val platform: Platform)
			}

			object Logoff : APIPostRequest<String>(this, "logoff")

			object UpdateToken : APIPost<String, String>(this, "updateToken")

			object QueryTokenMutexMap: APIGetResponse<List<String>>(this, "queryTokenMutexMap")

			object Register : APIPostRequest<Register.Request>(this, "register") {
				@Serializable
				data class Request(val name: String, val pwd: String, val inviterName: String)
			}

			object ForgotPassword : APIPostRequest<ForgotPassword.Request>(this, "forgotPassword") {
				@Serializable
				data class Request(val name: String, val pwd: String)
			}

			object ChangePassword : APIPostRequest<ChangePassword.Request>(this,"changePassword") {
				@Serializable
				data class Request(val token: String, val oldPwd: String, val newPwd: String)
			}
		}

		object Activity : APINode(this, "activity") {
			object GetActivities : APIPostResponse<List<love.yinlin.data.rachel.activity.Activity>>(this, "getActivities")

			object AddActivity : APIForm<AddActivity.Request, AddActivity.Response, AddActivity.Files>(this, "addActivity") {
				@Serializable
				data class Request(val token: String, val activity: love.yinlin.data.rachel.activity.Activity)
				@Serializable
				data class Files(val pic: APIFile?, val pics: APIFiles)
				@Serializable
				data class Response(val aid: Int, val pic: String?, val pics: List<String>)
			}

			object ModifyActivityInfo : APIPostRequest<ModifyActivityInfo.Request>(this, "modifyActivityInfo") {
				@Serializable
				data class Request(val token: String, val activity: love.yinlin.data.rachel.activity.Activity)
			}

			object ModifyActivityPicture : APIForm<ModifyActivityPicture.Request, String, ModifyActivityPicture.Files>(this, "modifyActivityPicture") {
				@Serializable
				data class Request(val token: String, val aid: Int)
				@Serializable
				data class Files(val pic: APIFile)
			}

			object DeleteActivityPicture : APIPostRequest<DeleteActivityPicture.Request>(this, "deleteActivityPicture") {
				@Serializable
				data class Request(val token: String, val aid: Int)
			}

			object AddActivityPictures : APIForm<AddActivityPictures.Request, List<String>, AddActivityPictures.Files>(this, "addActivityPictures") {
				@Serializable
				data class Request(val token: String, val aid: Int)
				@Serializable
				data class Files(val pics: APIFiles)
			}

			object ModifyActivityPictures : APIForm<ModifyActivityPictures.Request, String, ModifyActivityPictures.Files>(this, "modifyActivityPictures") {
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
				data class Request(val token: String, val aid: Int)
			}
		}

		object Backup : APINode(this, "backup") {
			object UploadPlaylist : APIPostRequest<UploadPlaylist.Request>(this, "uploadPlaylist") {
				@Serializable
				data class Request(val token: String, val playlist: JsonObject)
			}

			object DownloadPlaylist : APIPost<String, JsonObject>(this, "downloadPlaylist")
		}

		object Follows : APINode(this, "follows") {
			object FollowUser : APIPostRequest<FollowUser.Request>(this, "followUser") {
				@Serializable
				data class Request(val token: String, val uid: Int)
			}

			object UnfollowUser : APIPostRequest<UnfollowUser.Request>(this, "unfollowUser") {
				@Serializable
				data class Request(val token: String, val uid: Int)
			}

			object GetFollows : APIPost<GetFollows.Request, List<FollowInfo>>(this, "getFollows") {
				@Serializable
				data class Request(val token: String, val score: Int = Int.MAX_VALUE, val fid: Long = 0L, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetFollowers : APIPost<GetFollowers.Request, List<FollowerInfo>>(this, "getFollowers") {
				@Serializable
				data class Request(val token: String, val score: Int = Int.MAX_VALUE, val fid: Long = 0L, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object BlockUser : APIPostRequest<BlockUser.Request>(this, "blockUser") {
				@Serializable
				data class Request(val token: String, val uid: Int)
			}

			object UnblockUser : APIPostRequest<UnblockUser.Request>(this, "unblockUser") {
				@Serializable
				data class Request(val token: String, val uid: Int)
			}

			object GetBlockedUsers : APIPost<GetBlockedUsers.Request, List<BlockedUserInfo>>(this, "getBlockedUsers") {
				@Serializable
				data class Request(val token: String, val fid: Long = 0L, val num: Int = APIConfig.MIN_PAGE_NUM)
			}
		}

		object Info : APINode(this, "info") {
			object SendFeedback : APIPostRequest<SendFeedback.Request>(this, "sendFeedback") {
				@Serializable
				data class Request(val token: String, val content: String)
			}
		}

		object Mail : APINode(this, "mail") {
			object GetMails : APIPost<GetMails.Request, List<love.yinlin.data.rachel.mail.Mail>>(this, "getMails") {
				@Serializable
				data class Request(val token: String, val isProcessed: Boolean = false, val mid: Long = Long.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object ProcessMail : APIPostRequest<ProcessMail.Request>(this, "processMail") {
				@Serializable
				data class Request(val token: String, val mid: Long, val confirm: Boolean)
			}

			object DeleteMail : APIPostRequest<DeleteMail.Request>(this, "deleteMail") {
				@Serializable
				data class Request(val token: String, val mid: Long)
			}
		}

		object Profile : APINode(this, "profile") {
			object GetProfile : APIPost<String, UserProfile>(this, "getProfile")

			object GetPublicProfile : APIPost<GetPublicProfile.Request, UserPublicProfile>(this, "getPublicProfile") {
				@Serializable
				data class Request(val token: String?, val uid: Int)
			}

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

			object ResetPicture : APIPostRequest<String>(this, "resetPicture")

			object Signin : APIPost<String, Signin.Response>(this, "signin") {
				@Serializable
				data class Response(val status: Boolean, val value: Int, val index: Int)
			}
		}

		object Topic : APINode(this, "topic") {
			object GetTopics : APIPost<GetTopics.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getTopics") {
				@Serializable
				data class Request(val uid: Int, val isTop: Boolean = true, val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetLatestTopics : APIPost<GetLatestTopics.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getLatestTopics") {
				@Serializable
				data class Request(val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetLatestTopicsByComment : APIPost<GetLatestTopicsByComment.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getLatestTopicsByComment") {
				@Serializable
				data class Request(val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetHotTopics : APIPost<GetHotTopics.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getHotTopics") {
				@Serializable
				data class Request(val score: Double = Double.MAX_VALUE, val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetSectionTopics : APIPost<GetSectionTopics.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getSectionTopics") {
				@Serializable
				data class Request(val section: Int, val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetTopicDetails : APIPost<Int, TopicDetails>(this, "getTopicDetails")

			object GetTopicComments : APIPost<GetTopicComments.Request, List<Comment>>(this, "getTopicComments") {
				@Serializable
				data class Request(val tid: Int, val rawSection: Int, val isTop: Boolean = true, val cid: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetTopicSubComments : APIPost<GetTopicSubComments.Request, List<SubComment>>(this, "getTopicSubComments") {
				@Serializable
				data class Request(val pid: Int, val rawSection: Int, val cid: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object SendTopic : APIForm<SendTopic.Request, SendTopic.Response, SendTopic.Files>(this, "sendTopic") {
				@Serializable
				data class Request(val token: String, val title: String, val content: String, val section: Int)
				@Serializable
				data class Files(val pics: APIFiles)
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

		object Song: APINode(this, "song") {
			object GetSongs : APIPost<GetSongs.Request, List<love.yinlin.data.rachel.song.Song>>(this, "getSongs") {
				@Serializable
				data class Request(val sid: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object SearchSong : APIPost<String, love.yinlin.data.rachel.song.Song>(this, "searchSong")

			object GetSongComments : APIPost<GetSongComments.Request, List<SongComment>>(this, "getSongComments") {
				@Serializable
				data class Request(val sid: Int, val cid: Long = 0L, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object SendSongComment : APIPost<SendSongComment.Request, Long>(this, "sendSongComment") {
				@Serializable
				data class Request(val token: String, val sid: Int, val content: String)
			}

			object DeleteSongComment : APIPostRequest<DeleteSongComment.Request>(this, "deleteSongComment") {
				@Serializable
				data class Request(val token: String, val cid: Long)
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
			data class Files(val c: APIFile)
		}
	}
}