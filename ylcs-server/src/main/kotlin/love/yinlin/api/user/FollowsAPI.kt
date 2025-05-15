package love.yinlin.api.user

import io.ktor.server.routing.Routing
import love.yinlin.api.API
import love.yinlin.api.ImplMap
import love.yinlin.api.api
import love.yinlin.api.successData
import love.yinlin.data.Data

fun Routing.followsAPI(implMap: ImplMap) {
    api(API.User.Follows.FollowUser) { (token, uid) ->
        "关注成功".successData
    }

    api(API.User.Follows.UnfollowUser) { (token, uid) ->
        "关注成功".successData
    }

    api(API.User.Follows.GetFollows) { (token, offset, num) ->
        Data.Success(emptyList())
    }

    api(API.User.Follows.GetFollowers) { (token, offset, num) ->
        Data.Success(emptyList())
    }

    api(API.User.Follows.BlockUser) { (token, uid) ->
        "拉黑成功".successData
    }

    api(API.User.Follows.UnblockUser) { (token, uid) ->
        "已取消拉黑".successData
    }

    api(API.User.Follows.GetBlockedUsers) { (token, offset, num) ->
        Data.Success(emptyList())
    }
}