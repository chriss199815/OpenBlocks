#version 150

// copy of the sky, taken right after it was drawn
uniform sampler2D Sampler0;

uniform vec2 ScreenSize;

out vec4 fragColor;

void main() {
    fragColor = vec4(texture(Sampler0, gl_FragCoord.xy / ScreenSize).rgb, 1.0);
}
