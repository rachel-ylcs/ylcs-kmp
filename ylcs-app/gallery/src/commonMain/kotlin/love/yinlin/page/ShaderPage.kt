package love.yinlin.page

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import love.yinlin.GalleryActions
import love.yinlin.GalleryCaption
import love.yinlin.LocalGalleryCompact
import love.yinlin.Page
import love.yinlin.PageGroup
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberTrue
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.graphics.ShaderProgram
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.text.Text
import org.intellij.lang.annotations.Language

@Stable
object ShaderPage : Page(
    id = "shader",
    icon = Icons.Code,
    title = "着色器",
    english = "Shaders",
    description = "实时预览四种程序化效果，并查看生成画面的 AGSL / SkSL 源码。",
    keywords = "ShaderProgram ShaderBrush AGSL SkSL uniform 片段着色器",
    group = PageGroup.Effects,
) {
    private val aurora = component("极光", "随时间摆动的多层光带，在星空背景上叠出冷色辉光。", "Aurora 星野 光带", previewAlias = "Aurora", tabOrder = 0)
    private val plasma = component("等离子", "叠加多方向的正弦波和径向波纹，生成连续流动的彩色纹理。", "Plasma 正弦波 余弦调色板", previewAlias = "Plasma", tabOrder = 1)
    private val voronoi = component("泰森多边形", "寻找最近与次近的种子点，为缓慢移动的多边形着色并勾出边界。", "Voronoi 细胞噪声 沃罗诺伊", previewAlias = "Voronoi", tabOrder = 2)
    private val mandelbrot = component("曼德勃罗集", "逐像素迭代复数，根据逃逸速度为分形边界着色。", "Mandelbrot Fractal 复数 分形", previewAlias = "Mandelbrot", tabOrder = 3)

    @Language("AGSL")
    private const val AURORA_SHADER = """
uniform float2 uResolution;
uniform float uTime;

float stars(float2 uv) {
    float2 grid = uv * 120.0;
    float seed = fract(sin(dot(floor(grid), float2(41.7, 289.3))) * 43758.5453);
    float spark = 1.0 - smoothstep(0.03, 0.19, length(fract(grid) - 0.5));
    return spark * smoothstep(0.975, 1.0, seed);
}

float ribbon(float2 uv, float offset) {
    float wave = 0.42 + offset
        + 0.10 * sin(uv.x * 4.0 + uTime * 0.35 + offset * 7.0)
        + 0.04 * sin(uv.x * 10.0 - uTime * 0.45);
    float distanceToRibbon = abs(uv.y - wave);
    float folds = 0.68 + 0.32 * sin(uv.x * 41.0 + sin(uv.x * 17.0) + uTime * 0.8);
    return exp(-distanceToRibbon * 16.0) * folds;
}

half4 main(float2 position) {
    float2 uv = position / uResolution;
    float2 starUv = position / uResolution.y;
    float3 sky = mix(float3(0.018, 0.025, 0.09), float3(0.035, 0.07, 0.16), uv.y);
    float3 light = ribbon(uv, -0.10) * float3(0.24, 0.13, 0.62);
    light += ribbon(uv, 0.04) * float3(0.03, 0.72, 0.43);
    light += ribbon(uv, 0.18) * float3(0.04, 0.27, 0.58);
    return half4(sky + light + float3(stars(starUv) * 0.75), 1.0);
}
""".trimIndent()

    @Language("AGSL")
    private const val PLASMA_SHADER = """
uniform float2 uResolution;
uniform float uTime;

half4 main(float2 position) {
    float2 p = (position - 0.5 * uResolution) / uResolution.y;
    float t = uTime * 0.65;
    float wave = sin(p.x * 8.0 + t);
    wave += sin(p.y * 11.0 - t * 1.2);
    wave += sin((p.x + p.y) * 7.0 + t * 0.8);
    wave += sin(length(p + 0.18 * float2(cos(t), sin(t))) * 16.0 - t * 1.6);

    float3 phase = float3(0.12, 1.85, 3.52);
    float3 color = 0.53 + 0.44 * cos(float3(wave * 0.87 + t * 0.4) + phase);
    return half4(color, 1.0);
}
""".trimIndent()

    @Language("AGSL")
    private const val VORONOI_SHADER = """
uniform float2 uResolution;
uniform float uTime;

float2 randomPair(float2 cell) {
    return fract(sin(float2(dot(cell, float2(127.1, 311.7)),
        dot(cell, float2(269.5, 183.3)))) * 43758.5453);
}

half4 main(float2 position) {
    float2 p = position / uResolution.y * 6.0;
    float2 cell = floor(p);
    float nearest = 10.0;
    float second = 10.0;
    float hue = 0.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            float2 neighbor = cell + float2(float(x), float(y));
            float2 seed = randomPair(neighbor);
            float2 point = neighbor + 0.5 + 0.32 * sin(seed * 6.283185 + uTime * 0.6);
            float distanceToSeed = length(point - p);
            if (distanceToSeed < nearest) {
                second = nearest;
                nearest = distanceToSeed;
                hue = seed.x;
            } else {
                second = min(second, distanceToSeed);
            }
        }
    }

    float3 tile = 0.53 + 0.36 * cos(float3(hue * 6.283185) + float3(0.4, 1.8, 3.2));
    float edge = 1.0 - smoothstep(0.015, 0.065, second - nearest);
    float dotGlow = 1.0 - smoothstep(0.025, 0.11, nearest);
    float3 color = mix(tile * (0.92 - 0.24 * nearest), float3(0.08, 0.09, 0.16), edge);
    return half4(mix(color, float3(1.0, 0.96, 0.82), dotGlow), 1.0);
}
""".trimIndent()

    @Language("AGSL")
    private const val MANDELBROT_SHADER = """
uniform float2 uResolution;
uniform float uTime;

half4 main(float2 position) {
    float2 uv = (position - 0.5 * uResolution) / uResolution.y;
    float zoom = exp2(0.65 - 0.65 * cos(uTime * 0.38));
    float2 c = float2(-0.70, 0.07) + uv * (2.45 / zoom);
    float2 z = float2(0.0);
    float escape = 0.0;

    for (int i = 0; i < 64; i++) {
        z = float2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c;
        float magnitude = dot(z, z);
        if (magnitude > 16.0) {
            escape = float(i) + 2.0 - log2(log2(magnitude));
            break;
        }
    }

    if (escape <= 0.0) return half4(0.025, 0.028, 0.075, 1.0);
    float3 band = 0.54 + 0.43 * cos(float3(escape * 0.26) + float3(0.2, 1.5, 3.0));
    float brightness = 0.24 + 0.76 * smoothstep(1.0, 23.0, escape);
    return half4(band * brightness, 1.0);
}
""".trimIndent()

    @Composable
    override fun Content() {
        ComponentColumn {
            Component(aurora) { ShaderPreview(AURORA_SHADER) }
            Component(plasma) { ShaderPreview(PLASMA_SHADER) }
            Component(voronoi) { ShaderPreview(VORONOI_SHADER) }
            Component(mandelbrot) { ShaderPreview(MANDELBROT_SHADER) }
        }
    }
}

@Composable
private fun ShaderPreview(source: String) {
    val program = remember(source) { ShaderProgram(source) }
    var playing by rememberTrue()
    var elapsed by rememberValueState(0f)

    LaunchedEffect(playing) {
        if (playing) {
            val previous = elapsed
            val start = withFrameNanos { it }
            while (isActive) {
                withFrameNanos { now ->
                    val frame = (now - start) / 33_333_333L
                    elapsed = previous + frame / 30f
                }
            }
        }
    }

    Box(
        Modifier.fillMaxWidth()
            .aspectRatio(if (LocalGalleryCompact.current) 1.55f else 2.65f)
            .clip(Theme.shape.v3)
            .drawWithCache {
                val brush = program.brush {
                    resolution("uResolution")
                    float("uTime", elapsed)
                }
                onDrawBehind { drawRect(brush) }
            },
    )

    GalleryActions {
        PrimaryTextButton(if (playing) "暂停动画" else "播放动画", onClick = { playing = !playing })
        GalleryCaption("AGSL / SkSL 源码")
    }

    SelectionContainer {
        Text(
            source,
            modifier = Modifier.fillMaxWidth()
                .clip(Theme.shape.v3)
                .background(Theme.color.backgroundVariant)
                .horizontalScroll(rememberScrollState())
                .padding(Theme.padding.value),
            color = Theme.color.onBackgroundVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            softWrap = false,
        )
    }
}
