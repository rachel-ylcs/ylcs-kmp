package love.yinlin.ui.component.platform

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush

@Stable
data class Shader(
    val speed: Float,
    val sksl: String
)

@Stable
interface ShaderEffect {
    fun setFloatUniform(name: String, value: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float)
    fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float)
    fun setFloatUniform(name: String, values: FloatArray)
    fun update(shader: Shader, time: Float, width: Float, height: Float)
    fun build(): Brush
}

expect fun makeShaderEffect(shader: Shader): ShaderEffect?

@Composable
fun ShaderBox(
    shader: Shader,
    modifier: Modifier,
    fallback: @Composable () -> Unit
) {
    val shaderEffect = remember(shader) { makeShaderEffect(shader) }
    if (shaderEffect != null) {
        val speed = shader.speed
        var startMillis = remember(shader) { -1L }
        val time by produceState(0f, speed) {
            while (true) {
                withInfiniteAnimationFrameMillis {
                    if (startMillis < 0) startMillis = it
                    value = ((it - startMillis) / 16.6f) / 10f
                }
            }
        }

        Box(modifier = modifier.drawBehind {
            shaderEffect.update(shader, time * speed, size.width, size.height)
            if (!size.isEmpty()) drawRect(brush = shaderEffect.build())
        })
    }
    else fallback()
}

@Stable
object Shaders {
    val GradientFlow = Shader(
        speed = 0.1f,
        sksl = """
uniform float uTime;
uniform vec3 uResolution;

mat2 Rot(float a)
{
    float s = sin(a);
    float c = cos(a);
    return mat2(c, -s, s, c);
}

// Created by inigo quilez - iq/2014
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
vec2 hash(vec2 p)
{
    p = vec2(dot(p, vec2(2127.1, 81.17)), dot(p, vec2(1269.5, 283.37)));
	return fract(sin(p) * 43758.5453);
}

float noise(in vec2 p)
{
    vec2 i = floor(p);
    vec2 f = fract(p);
	vec2 u = f * f * (3.0 - 2.0 * f);
    float n = mix(mix(dot(-1.0 + 2.0 * hash(i + vec2(0.0, 0.0)), f - vec2(0.0, 0.0)), 
                      dot(-1.0 + 2.0 * hash(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0)), u.x),
                  mix(dot(-1.0 + 2.0 * hash(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0)), 
                      dot(-1.0 + 2.0 * hash(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0)), u.x), u.y);
	return 0.5 + 0.5 * n;
}

vec4 main(vec2 fragCoord)
{
    vec2 uv = fragCoord / uResolution.xy;
    float ratio = uResolution.x / uResolution.y;
    vec2 tuv = uv;
    tuv -= .5;
    float degree = noise(vec2(uTime*.1, tuv.x*tuv.y));

    tuv.y *= 1./ratio;
    tuv *= Rot(radians((degree-.5)*720.+180.));
	tuv.y *= ratio;

    float frequency = 5.;
    float amplitude = 30.;
    float speed = uTime * 2.;
    tuv.x += sin(tuv.y*frequency+speed)/amplitude;
   	tuv.y += sin(tuv.x*frequency*1.5+speed)/(amplitude*.5);
    
    vec3 color1 = vec3(0.791, 0.496, 0.389);
    vec3 color2 = vec3(0.271, 0.608, 0.529);
    vec3 layer1 = mix(color1, color2, smoothstep(-.3, .2, (tuv*Rot(radians(-5.))).x));
    
    vec3 color3 = vec3(0.545, 0.412, 0.463);
    vec3 color4 = vec3(0.180, 0.216, 0.388);
    vec3 layer2 = mix(color3, color4, smoothstep(-.3, .2, (tuv*Rot(radians(-5.))).x));
    
    vec3 finalComp = mix(layer1, layer2, smoothstep(.5, -.3, tuv.y));
    
    vec3 col = finalComp;
    
    return vec4(col,1.0);
}"""
    )
}