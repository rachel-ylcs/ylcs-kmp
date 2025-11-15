package love.yinlin.api

val APIRes.url: String get() = "${ClientEngine.baseUrl}/$this"