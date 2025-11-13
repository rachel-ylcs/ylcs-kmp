package love.yinlin.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.rachel.follows.BlockedUserInfo
import love.yinlin.data.rachel.follows.FollowInfo
import love.yinlin.data.rachel.follows.FollowerInfo
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameRank
import love.yinlin.data.rachel.game.GameRecordWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.photo.PhotoAlbumList
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.data.rachel.server.ServerStatus
import love.yinlin.data.rachel.song.SongComment
import love.yinlin.data.rachel.song.SongPreview
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.SubComment
import love.yinlin.data.rachel.topic.TopicDetails
import love.yinlin.platform.Platform

// TODO: 迁移升级

data object ServerRes2 : ResNode2("public") {
	data object Activity : ResNode2(this, "activity") {
		fun activity(uniqueId: String) = ResNode2(this, "${uniqueId}.webp")
	}

	data object Assets : ResNode2(this, "assets") {
		val DefaultAvatar = ResNode2(this, "default_avatar.webp")

		val DefaultWall = ResNode2(this, "default_wall.webp")
	}

	data object Emoji : ResNode2(this, "emoji") {
		fun webp(id: Int) = ResNode2(this, "${id}.webp")
		fun lottie(id: Int) = ResNode2(this, "${id}.json")
	}

	data object Game : ResNode2(this, "game") {
		fun x(id: Int) = ResNode2(this, "game${id}x.webp")
		fun y(id: Int) = ResNode2(this, "game${id}y.webp")
		fun xy(id: Int, isX: Boolean) = if (isX) x(id) else y(id)
		fun res(id: Int, key: String) = ResNode2(this, "game$id$key.webp")
	}

	data object Photo : ResNode2(this, "photo") {
		fun pic(classification: String, index: Int, thumb: Boolean) = ResNode2(
			parent = ResNode2(this, classification),
			name = "$index${if (thumb) ".thumb" else ""}.webp"
		)
	}

	data object Mod : ResNode2(this, "mod") {
		class Song(sid: String) : ResNode2(this, sid) {
			fun res(filename: String) = ResNode2(this, filename)
		}
	}

	data object Users : ResNode2(this, "users") {
		class User(uid: Int) : ResNode2(this, "$uid") {
			val avatar = ResNode2(this, "avatar.webp")

			val wall = ResNode2(this, "wall.webp")

			inner class Pics : ResNode2(this, "pics") {
				fun pic(uniqueId: String) = ResNode2(this, "${uniqueId}.webp")
			}
		}
	}
}

object API2 : APINode2(null, "") {
	object Common : APINode2(this, "common") {
		object Status : APINode2(this, "status") {
			object GetServerStatus : APIGetResponse2<ServerStatus>(this, "getServerStatus")
		}

		object Photo : APINode2(this, "photo") {
			object SearchPhotoAlbums : APIPost2<SearchPhotoAlbums.Request, PhotoAlbumList>(this, "searchPhotoAlbums") {
				@Serializable
				data class Request(val keyword: String? = null, val ts: String = "2099-12-31", val aid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}
		}
	}

	object User : APINode2(this, "user") {
		object Account : APINode2(this, "account") {
			object GetInviters : APIPostResponse2<List<String>>(this, "getInviters")

			object Login : APIPost2<Login.Request, String>(this, "login") {
				@Serializable
				data class Request(val name: String, val pwd: String, val platform: Platform)
			}

			object Logoff : APIPostRequest2<String>(this, "logoff")

			object UpdateToken : APIPost2<String, String>(this, "updateToken")

			object Register : APIPostRequest2<Register.Request>(this, "register") {
				@Serializable
				data class Request(val name: String, val pwd: String, val inviterName: String)
			}

			object ForgotPassword : APIPostRequest2<ForgotPassword.Request>(this, "forgotPassword") {
				@Serializable
				data class Request(val name: String, val pwd: String)
			}

			object ChangePassword : APIPostRequest2<ChangePassword.Request>(this,"changePassword") {
				@Serializable
				data class Request(val token: String, val oldPwd: String, val newPwd: String)
			}
		}

		object Activity : APINode2(this, "activity") {
			object GetActivities : APIPostResponse2<List<love.yinlin.data.rachel.activity.Activity>>(this, "getActivities")

			object AddActivity : APIPost2<String, Int>(this, "addActivity")

			object UpdateActivityInfo : APIPostRequest2<UpdateActivityInfo.Request>(this, "updateActivityInfo") {
				@Serializable
				data class Request(val token: String, val activity: love.yinlin.data.rachel.activity.Activity)
			}

			object UpdateActivityPhoto : APIForm2<UpdateActivityPhoto.Request, String, UpdateActivityPhoto.Files>(this, "updateActivityPhoto") {
				@Serializable
				data class Request(val token: String, val aid: Int, val key: String)
				@Serializable
				data class Files(val pic: APIFile2)
			}

			object DeleteActivityPhoto : APIPostRequest2<DeleteActivityPhoto.Request>(this, "deleteActivityPhoto") {
				@Serializable
				data class Request(val token: String, val aid: Int, val key: String)
			}

			object AddActivityPhotos : APIForm2<AddActivityPhotos.Request, List<String>, AddActivityPhotos.Files>(this, "addActivityPhotos") {
				@Serializable
				data class Request(val token: String, val aid: Int, val key: String)
				@Serializable
				data class Files(val pics: APIFiles2)
			}

			object UpdateActivityPhotos : APIForm2<UpdateActivityPhotos.Request, String, UpdateActivityPhotos.Files>(this, "updateActivityPhotos") {
				@Serializable
				data class Request(val token: String, val aid: Int, val key: String, val index: Int)
				@Serializable
				data class Files(val pic: APIFile2)
			}

			object DeleteActivityPhotos : APIPostRequest2<DeleteActivityPhotos.Request>(this, "deleteActivityPhotos") {
				@Serializable
				data class Request(val token: String, val aid: Int, val key: String, val index: Int)
			}

			object DeleteActivity : APIPostRequest2<DeleteActivity.Request>(this, "deleteActivity") {
				@Serializable
				data class Request(val token: String, val aid: Int)
			}
		}

		object Backup : APINode2(this, "backup") {
			object UploadPlaylist : APIPostRequest2<UploadPlaylist.Request>(this, "uploadPlaylist") {
				@Serializable
				data class Request(val token: String, val playlist: JsonObject)
			}

			object DownloadPlaylist : APIPost2<String, JsonObject>(this, "downloadPlaylist")
		}

		object Follows : APINode2(this, "follows") {
			object FollowUser : APIPostRequest2<FollowUser.Request>(this, "followUser") {
				@Serializable
				data class Request(val token: String, val uid: Int)
			}

			object UnfollowUser : APIPostRequest2<UnfollowUser.Request>(this, "unfollowUser") {
				@Serializable
				data class Request(val token: String, val uid: Int)
			}

			object GetFollows : APIPost2<GetFollows.Request, List<FollowInfo>>(this, "getFollows") {
				@Serializable
				data class Request(val token: String, val score: Int = Int.MAX_VALUE, val fid: Long = 0L, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetFollowers : APIPost2<GetFollowers.Request, List<FollowerInfo>>(this, "getFollowers") {
				@Serializable
				data class Request(val token: String, val score: Int = Int.MAX_VALUE, val fid: Long = 0L, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object BlockUser : APIPostRequest2<BlockUser.Request>(this, "blockUser") {
				@Serializable
				data class Request(val token: String, val uid: Int)
			}

			object UnblockUser : APIPostRequest2<UnblockUser.Request>(this, "unblockUser") {
				@Serializable
				data class Request(val token: String, val uid: Int)
			}

			object GetBlockedUsers : APIPost2<GetBlockedUsers.Request, List<BlockedUserInfo>>(this, "getBlockedUsers") {
				@Serializable
				data class Request(val token: String, val fid: Long = 0L, val num: Int = APIConfig.MIN_PAGE_NUM)
			}
		}

		object Game : APINode2(this, "game") {
			object CreateGame : APIPost2<CreateGame.Request, Int>(this, "createGame") {
				@Serializable
				data class Request(val token: String, val title: String, val type: love.yinlin.data.rachel.game.Game, val reward: Int,
					val num: Int, val cost: Int, val info: JsonElement, val question: JsonElement, val answer: JsonElement)
			}

			object DeleteGame : APIPostRequest2<DeleteGame.Request>(this, "DeleteGame") {
				@Serializable
				data class Request(val token: String, val gid: Int)
			}

			object GetGames : APIPost2<GetGames.Request, List<GamePublicDetailsWithName>>(this, "getGames") {
				@Serializable
				data class Request(val type: love.yinlin.data.rachel.game.Game, val gid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetUserGames : APIPost2<GetUserGames.Request, List<GameDetailsWithName>>(this, "getUserGames") {
				@Serializable
				data class Request(val token: String, val gid: Int = Int.MAX_VALUE, val isCompleted: Boolean = false, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetUserGameRecords : APIPost2<GetUserGameRecords.Request, List<GameRecordWithName>>(this, "getUserGameRecords") {
				@Serializable
				data class Request(val token: String, val rid: Long = Long.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetGameRank : APIPost2<love.yinlin.data.rachel.game.Game, List<GameRank>>(this, "getGameRank")

			object PreflightGame : APIPost2<PreflightGame.Request, PreflightResult>(this, "preflightGame") {
				@Serializable
				data class Request(val token: String, val gid: Int)
			}

			object VerifyGame : APIPost2<VerifyGame.Request, GameResult>(this, "verifyGame") {
				@Serializable
				data class Request(val token: String, val gid: Int, val rid: Long, val answer: JsonElement)
			}
		}

		object Info : APINode2(this, "info") {
			object SendFeedback : APIPostRequest2<SendFeedback.Request>(this, "sendFeedback") {
				@Serializable
				data class Request(val token: String, val content: String)
			}
		}

		object Mail : APINode2(this, "mail") {
			object GetMails : APIPost2<GetMails.Request, List<love.yinlin.data.rachel.mail.Mail>>(this, "getMails") {
				@Serializable
				data class Request(val token: String, val isProcessed: Boolean = false, val mid: Long = Long.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object ProcessMail : APIPostRequest2<ProcessMail.Request>(this, "processMail") {
				@Serializable
				data class Request(val token: String, val mid: Long, val confirm: Boolean)
			}

			object DeleteMail : APIPostRequest2<DeleteMail.Request>(this, "deleteMail") {
				@Serializable
				data class Request(val token: String, val mid: Long)
			}
		}

		object Profile : APINode2(this, "profile") {
			object GetProfile : APIPost2<String, UserProfile>(this, "getProfile")

			object GetPublicProfile : APIPost2<GetPublicProfile.Request, UserPublicProfile>(this, "getPublicProfile") {
				@Serializable
				data class Request(val token: String?, val uid: Int)
			}

			object UpdateName : APIPostRequest2<UpdateName.Request>(this, "updateName") {
				@Serializable
				data class Request(val token: String, val name: String)
			}

			object UpdateAvatar : APIFormRequest2<String, UpdateAvatar.Files>(this, "updateAvatar") {
				@Serializable
				data class Files(val avatar: APIFile2)
			}

			object UpdateSignature : APIPostRequest2<UpdateSignature.Request>(this, "updateSignature") {
				@Serializable
				data class Request(val token: String, val signature: String)
			}

			object UpdateWall : APIFormRequest2<String, UpdateWall.Files>(this, "updateWall") {
				@Serializable
				data class Files(val wall: APIFile2)
			}

			object ResetPicture : APIPostRequest2<String>(this, "resetPicture")

			object Signin : APIPost2<String, Signin.Response>(this, "signin") {
				@Serializable
				data class Response(val status: Boolean, val value: Int, val index: Int)
			}
		}

		object Topic : APINode2(this, "topic") {
			object GetTopics : APIPost2<GetTopics.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getTopics") {
				@Serializable
				data class Request(val uid: Int, val isTop: Boolean = true, val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetLatestTopics : APIPost2<GetLatestTopics.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getLatestTopics") {
				@Serializable
				data class Request(val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetLatestTopicsByComment : APIPost2<GetLatestTopicsByComment.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getLatestTopicsByComment") {
				@Serializable
				data class Request(val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetHotTopics : APIPost2<GetHotTopics.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getHotTopics") {
				@Serializable
				data class Request(val score: Double = Double.MAX_VALUE, val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetSectionTopics : APIPost2<GetSectionTopics.Request, List<love.yinlin.data.rachel.topic.Topic>>(this, "getSectionTopics") {
				@Serializable
				data class Request(val section: Int, val tid: Int = Int.MAX_VALUE, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetTopicDetails : APIPost2<Int, TopicDetails>(this, "getTopicDetails")

			object GetTopicComments : APIPost2<GetTopicComments.Request, List<Comment>>(this, "getTopicComments") {
				@Serializable
				data class Request(val tid: Int, val rawSection: Int, val isTop: Boolean = true, val cid: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object GetTopicSubComments : APIPost2<GetTopicSubComments.Request, List<SubComment>>(this, "getTopicSubComments") {
				@Serializable
				data class Request(val pid: Int, val rawSection: Int, val cid: Int = 0, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object SendTopic : APIForm2<SendTopic.Request, SendTopic.Response, SendTopic.Files>(this, "sendTopic") {
				@Serializable
				data class Request(val token: String, val title: String, val content: String, val section: Int)
				@Serializable
				data class Files(val pics: APIFiles2)
				@Serializable
				data class Response(val tid: Int, val pic: String?)
			}

			object UpdateTopicTop : APIPostRequest2<UpdateTopicTop.Request>(this, "updateTopicTop") {
				@Serializable
				data class Request(val token: String, val tid: Int, val isTop: Boolean)
			}

			object DeleteTopic : APIPostRequest2<DeleteTopic.Request>(this, "deleteTopic") {
				@Serializable
				data class Request(val token: String, val tid: Int)
			}

			object MoveTopic : APIPostRequest2<MoveTopic.Request>(this, "moveTopic") {
				@Serializable
				data class Request(val token: String, val tid: Int, val section: Int)
			}

			object SendCoin : APIPostRequest2<SendCoin.Request>(this, "sendCoin") {
				@Serializable
				data class Request(val token: String, val uid: Int, val tid: Int, val value: Int)
			}

			object SendComment : APIPost2<SendComment.Request, Int>(this, "sendComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val rawSection: Int, val content: String)
			}

			object SendSubComment : APIPost2<SendSubComment.Request, Int>(this, "sendSubComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val cid: Int, val rawSection: Int, val content: String)
			}

			object UpdateCommentTop : APIPostRequest2<UpdateCommentTop.Request>(this, "updateCommentTop") {
				@Serializable
				data class Request(val token: String, val tid: Int, val cid: Int, val rawSection: Int, val isTop: Boolean)
			}

			object DeleteComment : APIPostRequest2<DeleteComment.Request>(this, "deleteComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val cid: Int, val rawSection: Int)
			}

			object DeleteSubComment : APIPostRequest2<DeleteSubComment.Request>(this, "deleteSubComment") {
				@Serializable
				data class Request(val token: String, val tid: Int, val pid: Int, val cid: Int, val rawSection: Int)
			}
		}

		object Song: APINode2(this, "song") {
			object GetSongs : APIPost2<GetSongs.Request, List<SongPreview>>(this, "getSongs") {
				@Serializable
				data class Request(val sid: String = "0", val num: Int = APIConfig.MAX_PAGE_NUM)
			}

			object GetSong : APIPost2<String, love.yinlin.data.rachel.song.Song>(this, "getSong")

			object SearchSongs : APIPost2<String, List<SongPreview>>(this, "searchSongs")

			object GetSongComments : APIPost2<GetSongComments.Request, List<SongComment>>(this, "getSongComments") {
				@Serializable
				data class Request(val sid: String, val cid: Long = 0L, val num: Int = APIConfig.MIN_PAGE_NUM)
			}

			object SendSongComment : APIPost2<SendSongComment.Request, Long>(this, "sendSongComment") {
				@Serializable
				data class Request(val token: String, val sid: String, val content: String)
			}

			object DeleteSongComment : APIPostRequest2<DeleteSongComment.Request>(this, "deleteSongComment") {
				@Serializable
				data class Request(val token: String, val cid: Long)
			}
		}
	}

	object Test : APINode2(this, "test") {
		object Get : APIGetRequest2<Get.Request>(this, "get") {
			@Serializable
			data class Request(val a: String)
		}

		object Post : APIFormRequest2<Post.Request, Post.Files>(this, "post") {
			@Serializable
			data class Request(val a: String, val b: Int)
			@Serializable
			data class Files(val c: APIFile2)
		}
	}
}