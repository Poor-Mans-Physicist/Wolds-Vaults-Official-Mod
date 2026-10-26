#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float strength = vertexColor.a * ColorModulator.a;
    if (strength < 0.003) {
        discard;
    }
    float seconds = GameTime * 1200.0;
    float angle = texCoord0.x * 6.2831853;
    float ripple = sin(angle * 7.0 + texCoord0.y * 18.0 - seconds * 11.0)
                 + 0.6 * sin(angle * 13.0 - texCoord0.y * 31.0 + seconds * 17.0);
    float swirl = cos(angle * 5.0 - texCoord0.y * 23.0 + seconds * 7.0);
    vec2 offset = vec2(ripple, swirl) * 0.011 * strength;
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    vec3 scene;
    scene.r = texture(Sampler0, clamp(uv + offset * 1.35, 0.0, 1.0)).r;
    scene.g = texture(Sampler0, clamp(uv + offset, 0.0, 1.0)).g;
    scene.b = texture(Sampler0, clamp(uv + offset * 0.7, 0.0, 1.0)).b;
    float crest = smoothstep(0.55, 1.0, abs(ripple) / 1.6);
    vec3 tinted = mix(scene, scene + vertexColor.rgb * 0.35, crest * strength);
    fragColor = vec4(tinted, clamp(strength * 3.0, 0.0, 1.0));
}
