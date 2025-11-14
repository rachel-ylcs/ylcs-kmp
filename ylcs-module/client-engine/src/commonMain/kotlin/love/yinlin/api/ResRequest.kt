package love.yinlin.api

val APIRes.uri: String get() = "${ClientEngine.baseUrl}/$this"