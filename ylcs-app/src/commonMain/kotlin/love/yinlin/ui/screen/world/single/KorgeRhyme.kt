package love.yinlin.ui.screen.world.single

import korlibs.audio.format.defaultAudioFormats
import korlibs.audio.format.flac.FLAC
import korlibs.audio.sound.Sound
import korlibs.audio.sound.readMusic
import korlibs.image.bitmap.Bitmap
import korlibs.image.color.RGBA
import korlibs.image.font.Font
import korlibs.image.font.readTtfFont
import korlibs.image.format.RegisteredImageFormats
import korlibs.image.format.WEBP
import korlibs.image.format.readBitmap
import korlibs.image.text.RichTextData
import korlibs.io.file.std.localVfs
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.input.onClickSuspend
import korlibs.korge.scene.Scene
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.*
import korlibs.korge.view.align.centerOnStage
import korlibs.math.geom.Size2D
import kotlinx.coroutines.delay
import love.yinlin.AppModel
import love.yinlin.platform.AppContext
import love.yinlin.platform.KorgeGame
import kotlin.random.Random

class RhymeGame(appModel: AppModel, appContext: AppContext) : KorgeGame(appModel, appContext) {
    override val mainScene: Scene get() = SceneLoading()

    lateinit var font: Font

    private suspend inline fun Container.uiFontButton(text: String, crossinline onClick: suspend () -> Unit) {
        uiButton(size = Size2D(200, 75)) {
            richText = RichTextData(text, font = font)
            onClickSuspend { onClick() }
        }
    }

    inner class SceneLoading : Scene() {
        override suspend fun SContainer.sceneMain() {
            // 初始化资源 , 注册格式
            RegisteredImageFormats.register(WEBP)
            defaultAudioFormats.register(FLAC)
            font = resourcesVfs["font/xwwk.ttf"].readTtfFont()

            text(text = "加载中 Loading...", textSize = 20, font = font).centerOnStage()
            delay(1000)

            sceneContainer.changeTo { SceneStart() }
        }
    }

    inner class SceneStart : Scene() {
        override suspend fun SContainer.sceneMain() {
            uiVerticalStack(padding = 25.0) {
                uiFontButton("1. 渲染随机颜色矩形") {
                    sceneContainer.changeTo { SceneTest1() }
                }
                uiFontButton("2. 渲染compose资源图片") {
                    sceneContainer.changeTo { SceneTest2() }
                }
                uiFontButton("3. 渲染本地图片") {
                    sceneContainer.changeTo { SceneTest3() }
                }
                uiFontButton("4. 播放compose资源音频") {
                    sceneContainer.changeTo { SceneTest4() }
                }
                uiFontButton("5. 关闭") {
                    gameWindow.close()
                }
            }
        }
    }

    inner class SceneTest1 : Scene() {
        override suspend fun SContainer.sceneMain() {
            uiVerticalStack(padding = 25.0) {
                solidRect(200, 200) {
                    color = RGBA(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))
                }
                uiFontButton("0. 返回") {
                    sceneContainer.changeTo { SceneStart() }
                }
            }
        }
    }

    inner class SceneTest2 : Scene() {
        private lateinit var img: Bitmap

        override suspend fun SContainer.sceneMain() {
            img = resourcesVfs["drawable/img_logo.webp"].readBitmap()
            image(img) {
                size = Size2D(250, 250)
                position(0, 100)
            }
            uiFontButton("0. 返回") {
                sceneContainer.changeTo { SceneStart() }
            }
        }
    }

    inner class SceneTest3 : Scene() {
        private lateinit var img: Bitmap

        override suspend fun SContainer.sceneMain() {
            try {
                img = localVfs("C:\\Users\\Administrator\\Desktop\\test.webp").readBitmap()
                image(img) {
                    size = Size2D(250, 250)
                    position(0, 100)
                }
            }
            catch (_: Throwable) {
                println("Error: 检查本地文件是否存在")
            }
            uiFontButton("0. 返回") {
                sceneContainer.changeTo { SceneStart() }
            }
        }
    }

    inner class SceneTest4 : Scene() {
        private lateinit var audio: Sound

        override suspend fun SContainer.sceneMain() {
            audio = resourcesVfs["files/test.flac"].readMusic()
            uiVerticalStack {
                uiFontButton("1. 播放") {
                    audio.play()
                }
                uiFontButton("0. 返回") {
                    sceneContainer.changeTo { SceneStart() }
                }
            }
        }
    }
}