#version 330 core

smooth in vec3 vertPos;
smooth in vec4 vertColour;
smooth in vec2 vertUv;
smooth in float vertTexId;

out vec4 outColour;

uniform sampler2D[16] tex;

void main() {
  outColour = texture(tex[int(vertTexId)], vertUv) * vertColour;
  //outColour = vertColour; //TODO
}
