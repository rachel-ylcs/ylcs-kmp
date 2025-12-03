package love.yinlin.common

import androidx.compose.runtime.Stable
import love.yinlin.compose.graphics.Shader

@Stable
data object Shaders {
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