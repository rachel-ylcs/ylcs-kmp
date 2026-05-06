package love.yinlin.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import love.yinlin.app
import love.yinlin.app.game_rhyme.resources.Res as RhymeRes
import love.yinlin.app.game_rhyme.resources.rhyme
import love.yinlin.app.game_rhyme.resources.music
import love.yinlin.app.global.resources.Res as GlobalRes
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.character.Character
import love.yinlin.compose.game.data.RhymeIllustration
import love.yinlin.compose.game.data.RhymePlayConfig
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.data.RhymeRankItem
import love.yinlin.compose.game.data.RhymeState
import love.yinlin.compose.game.viewport.Viewport
import love.yinlin.compose.game.plugin.AssetPlugin
import love.yinlin.compose.game.plugin.FontPlugin
import love.yinlin.compose.game.plugin.RhymePlugin
import love.yinlin.compose.game.plugin.ScenePlugin
import love.yinlin.compose.game.plugin.SoundPlugin
import love.yinlin.compose.game.ui.GameHelpLayout
import love.yinlin.compose.game.ui.GameRankLayout
import love.yinlin.compose.game.ui.GameSettlingLayout
import love.yinlin.compose.game.ui.RhymeCommonButton
import love.yinlin.compose.game.ui.RhymeIllustrationLayout
import love.yinlin.compose.game.ui.RhymeIllustrationSelector
import love.yinlin.compose.game.ui.RhymeMusicCard
import love.yinlin.compose.game.viewport.Camera
import love.yinlin.compose.graphics.decode
import love.yinlin.compose.rememberFontFamily
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.animation.WaveLoading
import love.yinlin.compose.ui.common.ArgsSlider
import love.yinlin.compose.ui.common.SliderArgs
import love.yinlin.compose.ui.common.value
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.HorizontalScrollContainer
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.input.SecondaryTextButton
import love.yinlin.compose.ui.node.BlurState
import love.yinlin.compose.ui.node.blurSource
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.coroutines.Coroutines
import love.yinlin.cs.*
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.data.rachel.rhyme.CharacterInfo
import love.yinlin.data.rachel.rhyme.RhymeDifficulty
import love.yinlin.data.rachel.rhyme.RhymePlayResult
import love.yinlin.data.rachel.rhyme.RhymeRepository
import love.yinlin.data.rachel.rhyme.RhymeUploadResult
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingError
import love.yinlin.extension.parseJsonValue
import love.yinlin.startup.StartupMusicPlayer
import kotlin.time.Duration.Companion.seconds

@Stable
class ScreenRhyme : BasicScreen() {
    private val blurState = BlurState()

    private val engine = Engine(
        viewport = Viewport.MatchHeight(2000),
        backgroundColor = Colors.Black,
        FontPlugin.ResourceFactory(
            GlobalRes.font.xwwk,
            RhymeRes.font.rhyme,
            RhymeRes.font.music,
        ),
        AssetPlugin.Factory(),
        ScenePlugin.Factory(
            cameraConfig = Camera.Config(),
            extraModifier = Modifier.blurSource(blurState)
        ),
        SoundPlugin.Factory(listOf()),
        RhymePlugin.Factory(
            context = app.rawContext,
            blurState = blurState,
            endListener = ::endGame
        )
    )

    private var gameState: RhymeState by mutableStateOf(RhymeState.Start)
    private var gameError: Boolean by mutableStateOf(false)

    private val library = mutableListOf<MusicInfo>()
    private var repository: RhymeRepository? by mutableStateOf(null)
    private val illustrationList by derivedStateOf {
        CharacterInfo.Pool.map { (id, info) ->
            RhymeIllustration(
                info = info,
                url = ServerRes.Game.Rhyme.CV.illustration(id).url,
                unlocked = if (info == CharacterInfo.Default) true else repository?.characters?.contains(id) == true
            )
        }
    }
    private val unlockedIllustrationList by derivedStateOf { illustrationList.filter { it.unlocked } }

    private suspend fun unlockCharacter(info: CharacterInfo) {
        val profile = app.config.userProfile
        val oldRepository = repository
        if (profile == null || oldRepository == null) slot.tip.warning("请先登录")
        else {
            val newCharacters = oldRepository.characters.toMutableList()
            if (info.id in newCharacters) slot.tip.warning("你已经解锁该立绘")
            else {
                val cost = info.cost
                if (profile.coin < cost) slot.tip.warning("你的银币不够哦")
                else {
                    ApiRhymeUnlockCharacter.request(app.config.userToken, info.id) {
                        newCharacters += info.id
                        repository = oldRepository.copy(characters = newCharacters)
                        app.config.userProfile = profile.copy(coin = profile.coin - cost, exp = profile.exp + cost / 2)
                        slot.tip.success("解锁成功")
                    }.warningTip
                }
            }
        }
    }

    private var isSubmit: Boolean = false

    private suspend fun loadRhymeRank(info: MusicInfo) {
        slot.loading.open(content = "正在加载排行榜") {
            ApiRhymeGetRank.request(app.config.userToken, info.id) { rankList ->
                gameState = RhymeState.Rank(info, RhymeRankItem.parse(rankList))
            }.errorTip
        }
    }

    private suspend fun submitResult(info: MusicInfo, playConfig: RhymePlayConfig, result: RhymePlayResult) {
        if (isSubmit) slot.tip.warning("不可重复上传成绩")
        else if (slot.confirm.open("确认上传此成绩并参与排行吗")) {
            val profile = app.config.userProfile
            if (profile == null) slot.tip.warning("请先登录")
            else {
                slot.loading.open(content = "正在上传成绩") {
                    val sid = info.id
                    val uid = profile.uid
                    val uploadResult = RhymeUploadResult.build(
                        uid,
                        sid,
                        DateEx.CurrentLong,
                        playConfig.difficulty.ordinal,
                        playConfig.character.id,
                        result
                    )
                    ApiRhymeUploadRecord.request(app.config.userToken, sid, uploadResult) {
                        isSubmit = true
                        slot.tip.success("提交成功")
                    }.errorTip
                }
            }
        }
    }

    private fun startGame(info: MusicInfo, playConfig: RhymePlayConfig) {
        if (engine.isRunning) return
        launch {
            slot.loading.open {
                catchingError {
                    val modPath = app.modPath
                    // 解析歌词文件
                    val lyricsText = info.path(modPath, ModResourceType.Rhyme).readText()
                    require(lyricsText != null) { "歌词资源文件丢失或损坏" }
                    val lyricsConfig = lyricsText.parseJsonValue<RhymeLyricsConfig>()
                    // 解析封面图片
                    val recordImage = Coroutines.io {
                        info.path(modPath, ModResourceType.Record).readByteArray()?.let(ImageBitmap::decode)
                    }
                    require(recordImage != null) { "封面资源文件丢失" }
                    require(lyricsConfig.id == info.id) { "歌词资源文件与MOD不匹配" }
                    // 音频路径
                    val audio = info.path(modPath, ModResourceType.Audio)
                    // 创建角色
                    val characterInfo = playConfig.character
                    val characterFactory = Character.Factory[characterInfo]
                    require(characterFactory != null) { "未知角色" }
                    val character = characterFactory()
                    val characterImage = Coroutines.io {
                        app.cache.loadByteArray(ServerRes.Game.Rhyme.CV.illustration(characterInfo.id).url)?.let(ImageBitmap::decode)
                    }

                    engine.plugin<RhymePlugin>().setupGame(
                        playInfo = RhymePlayInfo(
                            playConfig = playConfig,
                            musicInfo = info,
                            lyricsConfig = lyricsConfig,
                            musicRecord = recordImage,
                            characterCV =  characterImage
                        ),
                        character = character,
                        audio = audio
                    )
                    engine.isRunning = true
                    isSubmit = false
                    gameState = RhymeState.Playing(info, playConfig)
                }.errorTip
            }
        }
    }

    private fun endGame(result: RhymePlayResult?) {
        engine.isRunning = false
        gameState = if (result == null) RhymeState.Start else {
            val state = gameState
            if (state is RhymeState.Playing) RhymeState.Settling(state.info, state.playConfig, result) else RhymeState.Start
        }
    }

    override suspend fun initialize() {
        // 初始化曲库
        val rawLibrary = app.requireClassOrNull<StartupMusicPlayer>()?.library?.values
        if (rawLibrary != null) {
            Coroutines.io {
                val modPath = app.modPath
                rawLibrary.mapNotNullTo(library) { info ->
                    if (info.path(modPath, ModResourceType.Rhyme).exists()) info else null
                }
            }
        }
        // 初始化仓库
        Coroutines.io {
            val token = app.config.userToken
            if (token.isNotEmpty()) ApiRhymeGetUserRepository.request(token) { repository = it }
        }
        // 初始化游戏引擎
        if (!engine.initialize()) gameError = true
    }

    override fun finalize() {
        engine.release()
    }

    override fun onBack() {
        when (gameState) {
            is RhymeState.Start -> super.onBack()
            is RhymeState.Prepare, is RhymeState.Rank -> gameState = RhymeState.MusicLibrary
            is RhymeState.Playing -> engine.isRunning = false
            else -> gameState = RhymeState.Start
        }
    }

    @Composable
    private fun GameStartLayout() {
        val rhymeFont = rememberFontFamily(RhymeRes.font.rhyme)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            val rotateColors = remember { listOf(Colors.Green4, Colors.Red4, Colors.Orange4, Colors.Pink4, Colors.Purple4) }
            val rotateValues = remember { mutableStateListOf(0f, 0f, 0f, 0f, 0f) }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(1.seconds)
                    val (index1, index2) = rotateValues.indices.shuffled().take(2)
                    rotateValues[index1] += 45f
                    rotateValues[index2] -= 45f
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v1)
            ) {
                SimpleClipText(text = "Rhyme", style = Theme.typography.v1.bold.copy(fontFamily = rhymeFont))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h7, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(rotateValues.size) { index ->
                        val angle by animateFloatAsState(rotateValues[index])
                        Box(modifier = Modifier.size(Theme.size.icon).drawWithContent {
                            rotate(angle) {
                                drawRect(rotateColors[index])
                                drawRect(Colors.White, style = Stroke(2f))
                            }
                        })
                    }
                }
            }

            Column(
                modifier = Modifier.width(Theme.size.cell1),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v8)
            ) {
                val isInitialized = engine.isInitialized
                if (isInitialized) {
                    SimpleClipText(text = "横屏游玩体验更佳", color = Theme.color.primary, style = Theme.typography.v5.bold)
                }
                else if (gameError) {
                    SimpleClipText(text = "引擎加载失败", color = Theme.color.error, style = Theme.typography.v5.bold)
                }
                else {
                    WaveLoading.Content()
                    SimpleClipText(text = "正在加载中...", style = Theme.typography.v5.bold)
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                    maxItemsInEachRow = 2
                ) {
                    if (isInitialized) {
                        RhymeCommonButton(icon = Icons.LibraryMusic, text = "曲库", onClick = { gameState = RhymeState.MusicLibrary }, modifier = Modifier.weight(1f))
                        RhymeCommonButton(icon = Icons.AccountCircle, text = "立绘", onClick = { gameState = RhymeState.Illustration }, modifier = Modifier.weight(1f))
                        RhymeCommonButton(icon = Icons.Info, text = "帮助", onClick = { gameState = RhymeState.Help }, modifier = Modifier.weight(1f))
                    }
                    RhymeCommonButton(icon = Icons.ArrowBack, text = "返回", onClick = ::onBack, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    @Composable
    private fun GameMusicLibraryLayout() {
        Column(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleClipText(text = "曲库", style = Theme.typography.v4.bold, modifier = Modifier.padding(Theme.padding.value7))
                ActionScope.Right.Container(modifier = Modifier.weight(1f).padding(Theme.padding.value9)) {
                    Icon(icon = Icons.ArrowBack, tip = "返回", onClick = ::onBack)
                }
            }

            if (library.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SimpleClipText(text = "曲库MOD未下载游戏配置", style = Theme.typography.v5.bold)
                }
            }
            else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Theme.size.cell1),
                    contentPadding = Theme.padding.eValue10,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.e10),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e10),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(items = library, key = { it.id }) { info ->
                        RhymeMusicCard(info = info, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PrimaryTextButton(text = "开始", icon = Icons.PlayArrow, onClick = { gameState = RhymeState.Prepare(info) })
                                SecondaryTextButton(text = "排行", icon = Icons.RewardCup, onClick = {
                                    launch { loadRhymeRank(info) }
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GameIllustrationLayout() {
        Column(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleClipText(text = "立绘", style = Theme.typography.v4.bold, modifier = Modifier.padding(Theme.padding.value7))
                ActionScope.Right.Container(modifier = Modifier.weight(1f).padding(Theme.padding.value9)) {
                    Icon(icon = Icons.ArrowBack, tip = "返回", onClick = { gameState = RhymeState.Start })
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.cell4),
                contentPadding = Theme.padding.eValue10,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.e10),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e10),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(items = illustrationList, key = { it.info.id }) { illustration ->
                    RhymeIllustrationLayout(
                        illustration = illustration,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        onClick = { characterSheet.open(illustration) }
                    )
                }
            }
        }
    }

    @Composable
    private fun GamePrepareLayout(info: MusicInfo) {
        var difficulty by rememberState { RhymePlayConfig.Default.difficulty }
        var audioDelay by rememberState { SliderArgs(0L, RhymePlayConfig.MIN_AUDIO_DELAY, RhymePlayConfig.MAX_AUDIO_DELAY) }
        var character by rememberState { RhymePlayConfig.Default.character }

        Column(modifier = Modifier.fillMaxSize().padding(LocalImmersivePadding.current)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleClipText(text = "准备", style = Theme.typography.v4.bold, modifier = Modifier.padding(Theme.padding.value7))
                ActionScope.Right.Container(modifier = Modifier.weight(1f).padding(Theme.padding.value9)) {
                    Icon(icon = Icons.ArrowBack, tip = "返回", onClick = { gameState = RhymeState.MusicLibrary })
                    Icon(icon = Icons.PlayArrow, tip = "开始", onClick = {
                        startGame(info, RhymePlayConfig(
                            difficulty = difficulty,
                            audioDelay = audioDelay.value,
                            character = character
                        ))
                    })
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
            ) {
                RhymeMusicCard(info = info, modifier = Modifier.widthIn(min = Theme.size.cell1, max = Theme.size.cell1 * 1.5f)) { }

                SimpleClipText(text = "难度", style = Theme.typography.v6.bold)

                Filter(
                    size = RhymeDifficulty.entries.size,
                    selectedProvider = { difficulty == RhymeDifficulty.entries[it] },
                    titleProvider = { RhymeDifficulty.entries[it].title },
                    onClick = { index, selected -> if (selected) difficulty = RhymeDifficulty.entries[index] }
                )

                ArgsSlider(
                    title = "延迟补偿(毫秒)",
                    args = audioDelay,
                    onValueChange = { audioDelay = audioDelay.copy(tmpValue = it) },
                    modifier = Modifier.widthIn(min = Theme.size.cell1, max = Theme.size.cell1 * 1.5f)
                )

                SimpleClipText(text = "立绘", style = Theme.typography.v6.bold)

                val state = rememberLazyListState()
                HorizontalScrollContainer(state, modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        state = state,
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    ) {
                        items(items = unlockedIllustrationList, key = { it.info.id }) { illustration ->
                            RhymeIllustrationSelector(
                                illustration = illustration,
                                checked = character == illustration.info,
                                modifier = Modifier.width(Theme.size.cell6).aspectRatio(1f),
                                onClick = { character = illustration.info }
                            )
                        }
                    }
                }

                Text(text = remember(character) { character.skill })
            }
        }
    }

    @Composable
    override fun BasicContent() {
        Theme.ThemeModeWrapper(true) {
            AnimationContent(gameState, modifier = Modifier.fillMaxSize().background(Theme.color.background)) { state ->
                when (state) {
                    is RhymeState.Start -> GameStartLayout()
                    is RhymeState.MusicLibrary -> GameMusicLibraryLayout()
                    is RhymeState.Illustration -> GameIllustrationLayout()
                    is RhymeState.Help -> GameHelpLayout()
                    is RhymeState.Prepare -> GamePrepareLayout(state.info)
                    is RhymeState.Playing -> engine.ViewportContent(modifier = Modifier.fillMaxSize(), padding = LocalImmersivePadding.current)
                    is RhymeState.Settling -> GameSettlingLayout(state, ::onBack) {
                        launch {
                            submitResult(state.info, state.playConfig, state.result)
                        }
                    }
                    is RhymeState.Rank -> GameRankLayout(state.info, state.map)
                }
            }
        }
        engine.PreloadEnvironment()
    }

    private val characterSheet = this land object : SheetContent<RhymeIllustration>() {
        override val maxPortraitRatio: Float = 0.8f

        @Composable
        override fun Content(args: RhymeIllustration) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
            ) {
                val info = args.info
                val unlocked = args.unlocked

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WebImage(
                        uri = args.url,
                        key = info.id,
                        modifier = Modifier.size(Theme.size.image6).clip(Theme.shape.v5)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
                    ) {
                        SimpleEllipsisText(text = info.title, style = Theme.typography.v5.bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SimpleEllipsisText(
                                text = if (unlocked) "已解锁" else "银币: ${info.cost}",
                                color = if (unlocked) Colors.Green4 else Colors.Purple4,
                                style = Theme.typography.v6
                            )
                            if (!unlocked) {
                                PrimaryTextButton(text = "解锁", icon = Icons.Store, onClick = {
                                    launch { unlockCharacter(info) }
                                })
                            }
                        }
                    }
                }

                Text(text = info.description)

                SimpleClipText(text = "琴韵", color = Theme.color.primary, style = Theme.typography.v5.bold)

                Text(text = remember(info) { info.skill }, color = Theme.color.secondary)
            }
        }
    }
}