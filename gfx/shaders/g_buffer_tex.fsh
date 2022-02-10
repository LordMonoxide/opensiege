#version 330 core

layout (location = 0) out vec3 gPosition;
layout (location = 1) out vec3 gNormal;
layout (location = 2) out vec4 gColour;

smooth in vec3 vertPos;
smooth in vec3 vertNormal;
smooth in vec2 vertUv;

uniform sampler2D texDiffuse;
//TODO uniform sampler2D texSpecular;

void main() {
  gPosition = vertPos;
  gNormal = normalize(vertNormal);
  gColour.rgb = texture(texDiffuse, vertUv).rgb;
  gColour.a = 0.0; //TODO texture(texSpecular, vertUv).r;
}
