-printmapping build/desktop-mapping.txt

# ----------------------------------------- JavaFx ----------------------------------------------- #

-keep class com.sun.** { *; }

# ----------------------------------------- App ----------------------------------------------- #

-keep class love.yinlin.data.weibo.WeiboAlbum$** {*;}
-keep class love.yinlin.ui.Route$**{*;}