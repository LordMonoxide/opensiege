#version 330 core

smooth in vec2 vertUv;

out vec4 outColour;

uniform sampler2D tex;

void main() {
  // Gamma correct
  float gamma = 2.2;

  outColour.rgb = pow(texture(tex, vertUv).rgb, vec3(1.0 / gamma));
}
