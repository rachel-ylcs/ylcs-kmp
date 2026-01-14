package love.yinlin.cs

val APIRes.url: String get() = "${ClientEngine.baseUrl}/$this"