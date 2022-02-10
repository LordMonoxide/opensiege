#version 330 core

smooth in vec3 vertPos;
smooth in vec4 vertColour;
smooth in vec2 vertUv;

out vec4 outColour;

uniform sampler2D tex;

void main() {
  outColour = texture(tex, vertUv) * vertColour;
  outColour = vertColour; //TODO
}
