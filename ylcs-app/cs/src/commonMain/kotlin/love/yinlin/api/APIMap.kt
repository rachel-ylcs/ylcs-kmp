package love.yinlin.api

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import love.yinlin.data.rachel.activity.Activity
import love.yinlin.data.rachel.follows.BlockedUserInfo
import love.yinlin.data.rachel.follows.FollowInfo
import love.yinlin.data.rachel.follows.FollowerInfo
import love.yinlin.data.rachel.game.CreateGameArgs
import love.yinlin.data.rachel.game.Game
import love.yinlin.data.rachel.game.GameDetailsWithName
import love.yinlin.data.rachel.game.GamePublicDetailsWithName
import love.yinlin.data.rachel.game.GameRank
import love.yinlin.data.rachel.game.GameRecordWithName
import love.yinlin.data.rachel.game.GameResult
import love.yinlin.data.rachel.game.PreflightResult
import love.yinlin.data.rachel.mail.Mail
import love.yinlin.data.rachel.photo.PhotoAlbumList
import love.yinlin.data.rachel.profile.UserProfile
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.data.rachel.server.ServerStatus
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.song.SongComment
import love.yinlin.data.rachel.song.SongPreview
import love.yinlin.data.rachel.topic.Comment
import love.yinlin.data.rachel.topic.SubComment
import love.yinlin.data.rachel.topic.Topic
import love.yinlin.data.rachel.topic.TopicDetails
import love.yinlin.platform.Platform
import love.yinlin.data.rachel.rhyme.RhymeRank
import love.yinlin.data.rachel.prize.Prize
import love.yinlin.data.rachel.prize.PrizeItemdata
import love.yinlin.data.rachel.prize.PrizeDraw
import love.yinlin.data.rachel.prize.Prizedata

// Common

val ApiCommonGetServerStatus by API.post.i().o<ServerStatus>()

@APIParam("token")
@APIParam("content")
val ApiCommonSendFeedback by API.post.i<String, String>().o()

// Photo

@APIParam("keyword", default = "null")
@APIParam("ts", default = "2099-12-31")
@APIParam("aid", default = "Int.MAX_VALUE")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiPhotoSearchPhotoAlbums by API.post.i<String?, String, Int, Int>().o<PhotoAlbumList>()

// Account

val ApiAccountGetInviters by API.post.i().o<List<String>>()

@APIParam("name")
@APIParam("pwd")
@APIParam("platform")
@APIReturn("token")
val ApiAccountLogin by API.post.i<String, String, Platform>().o<String>()

@APIParam("token")
val ApiAccountLogOff by API.post.i<String>().o()

@APIParam("token")
@APIReturn("Boolean")
val ApiAccountValidateToken by API.post.i<String>().o<Boolean>()

@APIParam("token")
@APIReturn("newToken")
val ApiAccountUpdateToken by API.post.i<String>().o<String>()

@APIParam("name")
@APIParam("pwd")
@APIParam("inviterName")
val ApiAccountRegister by API.post.i<String, String, String>().o()

@APIParam("name")
@APIParam("pwd")
val ApiAccountForgotPassword by API.post.i<String, String>().o()

@APIParam("token")
@APIParam("oldPwd")
@APIParam("newPwd")
val ApiAccountChangePassword by API.post.i<String, String, String>().o()

// Activity

@APIParam("token")
val ApiActivityGetActivities by API.post.i<String?>().o<List<Activity>>()

@APIParam("token")
@APIParam("hide")
@APIReturn("aid")
val ApiActivityAddActivity by API.post.i<String, Boolean>().o<Int>()

@APIParam("token")
@APIParam("activity")
val ApiActivityUpdateActivityInfo by API.post.i<String, Activity>().o()

@APIParam("token")
@APIParam("aid")
@APIParam("key")
@APIParam("pic")
@APIReturn("filename")
val ApiActivityUpdateActivityPhoto by API.form.i<String, Int, String, APIFile>().o<String>()

@APIParam("token")
@APIParam("aid")
@APIParam("key")
val ApiActivityDeleteActivityPhoto by API.post.i<String, Int, String>().o()

@APIParam("token")
@APIParam("aid")
@APIParam("key")
@APIParam("pics")
@APIReturn("filenames")
val ApiActivityAddActivityPhotos by API.form.i<String, Int, String, APIFile>().o<List<String>>()

@APIParam("token")
@APIParam("aid")
@APIParam("key")
@APIParam("index")
@APIParam("pic")
@APIReturn("filename")
val ApiActivityUpdateActivityPhotos by API.form.i<String, Int, String, Int, APIFile>().o<String>()

@APIParam("token")
@APIParam("aid")
@APIParam("key")
@APIParam("index")
val ApiActivityDeleteActivityPhotos by API.post.i<String, Int, String, Int>().o()

@APIParam("token")
@APIParam("aid")
val ApiActivityDeleteActivity by API.post.i<String, Int>().o()

// Backup

@APIParam("token")
@APIParam("playlist")
val ApiBackupUploadPlaylist by API.post.i<String, JsonObject>().o()

@APIParam("token")
@APIReturn("playlist")
val ApiBackupDownloadPlaylist by API.post.i<String>().o<JsonObject>()

// Follows

@APIParam("token")
@APIParam("uid")
val ApiFollowsFollowUser by API.post.i<String, Int>().o()

@APIParam("token")
@APIParam("uid")
val ApiFollowsUnfollowUser by API.post.i<String, Int>().o()

@APIParam("token")
@APIParam("score", default = "Int.MAX_VALUE")
@APIParam("fid", default = "0L")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiFollowsGetFollows by API.post.i<String, Int, Long, Int>().o<List<FollowInfo>>()

@APIParam("token")
@APIParam("score", default = "Int.MAX_VALUE")
@APIParam("fid", default = "0L")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiFollowsGetFollowers by API.post.i<String, Int, Long, Int>().o<List<FollowerInfo>>()

@APIParam("token")
@APIParam("uid")
val ApiFollowsBlockUser by API.post.i<String, Int>().o()

@APIParam("token")
@APIParam("uid")
val ApiFollowsUnblockUser by API.post.i<String, Int>().o()

@APIParam("token")
@APIParam("fid", default = "0L")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiFollowsGetBlockedUsers by API.post.i<String, Long, Int>().o<List<BlockedUserInfo>>()

// Game

@APIParam("token")
@APIParam("args")
@APIParam("gid")
val ApiGameCreateGame by API.post.i<String, CreateGameArgs>().o<Int>()

@APIParam("token")
@APIParam("gid")
val ApiGameDeleteGame by API.post.i<String, Int>().o()

@APIParam("type")
@APIParam("gid", default = "Int.MAX_VALUE")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiGameGetGames by API.post.i<Game, Int, Int>().o<List<GamePublicDetailsWithName>>()

@APIParam("token")
@APIParam("gid", default = "Int.MAX_VALUE")
@APIParam("isCompleted", default = "false")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiGameGetUserGames by API.post.i<String, Int, Boolean, Int>().o<List<GameDetailsWithName>>()

@APIParam("token")
@APIParam("rid", default = "Long.MAX_VALUE")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiGameGetUserGameRecords by API.post.i<String, Long, Int>().o<List<GameRecordWithName>>()

@APIParam("type")
val ApiGameGetGameRank by API.post.i<Game>().o<List<GameRank>>()

@APIParam("token")
@APIParam("gid")
val ApiGamePreflightGame by API.post.i<String, Int>().o<PreflightResult>()

@APIParam("token")
@APIParam("gid")
@APIParam("rid")
@APIParam("answer")
val ApiGameVerifyGame by API.post.i<String, Int, Long, JsonElement>().o<GameResult>()

// Mail

@APIParam("token")
@APIParam("isProcessed", default = "false")
@APIParam("mid", default = "Long.MAX_VALUE")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiMailGetMails by API.post.i<String, Boolean, Long, Int>().o<List<Mail>>()

@APIParam("token")
@APIParam("mid")
@APIParam("confirm")
@APIReturn("message")
val ApiMailProcessMail by API.post.i<String, Long, Boolean>().o<String>()

@APIParam("token")
@APIParam("mid")
val ApiMailDeleteMail by API.post.i<String, Long>().o()

// Profile

@APIParam("token")
val ApiProfileGetProfile by API.post.i<String>().o<UserProfile>()

@APIParam("token")
@APIParam("uid")
val ApiProfileGetPublicProfile by API.post.i<String?, Int>().o<UserPublicProfile>()

@APIParam("token")
@APIParam("name")
val ApiProfileUpdateName by API.post.i<String, String>().o()

@APIParam("token")
@APIParam("signature")
val ApiProfileUpdateSignature by API.post.i<String, String>().o()

@APIParam("token")
@APIParam("avatar")
val ApiProfileUpdateAvatar by API.form.i<String, APIFile>().o()

@APIParam("token")
@APIParam("wall")
val ApiProfileUpdateWall by API.form.i<String, APIFile>().o()

@APIParam("token")
val ApiProfileResetPicture by API.post.i<String>().o()

@APIParam("token")
@APIReturn("status")
@APIReturn("value")
@APIReturn("index")
val ApiProfileSignin by API.post.i<String>().o<Boolean, Int, Int>()

// Topic

@APIParam("uid")
@APIParam("isTop", default = "true")
@APIParam("tid", default = "Int.MAX_VALUE")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiTopicGetTopics by API.post.i<Int, Boolean, Int, Int>().o<List<Topic>>()

@APIParam("tid", default = "Int.MAX_VALUE")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiTopicGetLatestTopics by API.post.i<Int, Int>().o<List<Topic>>()

@APIParam("tid", default = "Int.MAX_VALUE")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiTopicGetLatestTopicsByComment by API.post.i<Int, Int>().o<List<Topic>>()

@APIParam("score", default = "Double.MAX_VALUE")
@APIParam("tid", default = "Int.MAX_VALUE")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiTopicGetHotTopics by API.post.i<Double, Int, Int>().o<List<Topic>>()

@APIParam("section")
@APIParam("tid", default = "Int.MAX_VALUE")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiTopicGetSectionTopics by API.post.i<Int, Int, Int>().o<List<Topic>>()

@APIParam("tid")
val ApiTopicGetTopicDetails by API.post.i<Int>().o<TopicDetails>()

@APIParam("tid")
@APIParam("rawSection")
@APIParam("isTop", default = "true")
@APIParam("cid", default = "0")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiTopicGetTopicComments by API.post.i<Int, Int, Boolean, Int, Int>().o<List<Comment>>()

@APIParam("pid")
@APIParam("rawSection")
@APIParam("cid", default = "0")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiTopicGetTopicSubComments by API.post.i<Int, Int, Int, Int>().o<List<SubComment>>()

@APIParam("token")
@APIParam("title")
@APIParam("content")
@APIParam("section")
@APIParam("pics")
@APIReturn("tid")
@APIReturn("pic")
val ApiTopicSendTopic by API.form.i<String, String, String, Int, APIFile?>().o<Int, String?>()

@APIParam("token")
@APIParam("tid")
@APIParam("isTop")
val ApiTopicUpdateTopicTop by API.post.i<String, Int, Boolean>().o()

@APIParam("token")
@APIParam("tid")
val ApiTopicDeleteTopic by API.post.i<String, Int>().o()

@APIParam("token")
@APIParam("tid")
@APIParam("section")
val ApiTopicMoveTopic by API.post.i<String, Int, Int>().o()

@APIParam("token")
@APIParam("uid")
@APIParam("tid")
@APIParam("value")
val ApiTopicSendCoin by API.post.i<String, Int, Int, Int>().o()

@APIParam("token")
@APIParam("tid")
@APIParam("rawSection")
@APIParam("content")
@APIReturn("cid")
val ApiTopicSendComment by API.post.i<String, Int, Int, String>().o<Int>()

@APIParam("token")
@APIParam("tid")
@APIParam("cid")
@APIParam("rawSection")
@APIParam("content")
@APIReturn("cid")
val ApiTopicSendSubComment by API.post.i<String, Int, Int, Int, String>().o<Int>()

@APIParam("token")
@APIParam("tid")
@APIParam("cid")
@APIParam("rawSection")
@APIParam("isTop")
val ApiTopicUpdateCommentTop by API.post.i<String, Int, Int, Int, Boolean>().o()

@APIParam("token")
@APIParam("tid")
@APIParam("cid")
@APIParam("rawSection")
val ApiTopicDeleteComment by API.post.i<String, Int, Int, Int>().o()

@APIParam("token")
@APIParam("tid")
@APIParam("pid")
@APIParam("cid")
@APIParam("rawSection")
val ApiTopicDeleteSubComment by API.post.i<String, Int, Int, Int, Int>().o()

// Song

@APIParam("sid", default = "0")
@APIParam("num", default = "APIConfig.MAX_PAGE_NUM")
val ApiSongGetSongs by API.post.i<String, Int>().o<List<SongPreview>>()

@APIParam("sid")
val ApiSongGetSong by API.post.i<String>().o<Song>()

@APIParam("key")
val ApiSongSearchSongs by API.post.i<String>().o<List<SongPreview>>()

@APIParam("sid")
@APIParam("cid", default = "0L")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiSongGetSongComments by API.post.i<String, Long, Int>().o<List<SongComment>>()

@APIParam("token")
@APIParam("sid")
@APIParam("content")
@APIReturn("cid")
val ApiSongSendSongComment by API.post.i<String, String, String>().o<Long>()

@APIParam("token")
@APIParam("cid")
val ApiSongDeleteSongComment by API.post.i<String, Long>().o()

//prize
@APIParam("pid")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiPrizeGetPrize by API.post.i<Int, Int>().o<List<Prize>>()

@APIParam("token")
@APIParam("Prizedata")
val ApiPrizeCreatePrize by API.post.i<String, Prizedata>().o<Int>()

@APIParam("token")
@APIParam("pid")
@APIParam("Prizedata")
val ApiPrizeUpdatePrize by API.post.i<String, Int, Prizedata>().o()

@APIParam("token")
@APIParam("pid")
val ApiPrizeDeletePrize by API.post.i<String, Int>().o<Int>()

@APIParam("token")
@APIParam("pid")
@APIParam("pic")
@APIParam("PrizeItemdata")
val ApiPrizeAddItem by API.form.i<String, Int, APIFile?, PrizeItemdata>().o<Int>()

@APIParam("token")
@APIParam("pid")
@APIParam("itemID")
val ApiPrizeDeleteItem by API.post.i<String, Int, Int>().o()

@APIParam("token")
@APIParam("itemID")
@APIParam("pic")
@APIParam("PrizeItemdata")
val ApiPrizeUpdateItem by API.form.i<String, Int, APIFile?, PrizeItemdata>().o<Int>()

@APIParam("token")
@APIParam("pid")
val ApiPrizeParticipate by API.post.i<String, Int>().o()


@APIParam("pid")
@APIReturn("Map<Int, List<Int>>") // {itemid1:[uid1,uid2], itemid2:[uid3], ...}
val ApiPrizeGetWinners by API.post.i<Int>().o<String>()


@APIParam("token")
@APIParam("pid")
val ApiPrizeDrawPrize by API.post.i<String, Int>().o<String>()
//管理员手动开奖，生成中奖名单

@APIParam("token")
@APIParam("sid")
@APIParam("difficulty")
@APIParam("score")
val ApiRhymeUploadRecord by API.post.i<String, Int, Int, Int>().o()

@APIParam("sid")
@APIParam("difficulty")
@APIParam("score")
@APIParam("uid", default = "0")
@APIParam("num", default = "APIConfig.MIN_PAGE_NUM")
val ApiRhymeGetSongRank by API.post.i<Int, Int, Int, Int, Int>().o<List<RhymeRank>>()



