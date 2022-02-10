#version 330 core

layout(location = 0) in vec3 inPos;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec4 inColour;
layout(location = 3) in vec2 inUv;
layout(location = 4) in float inTexId;

smooth out vec3 vertPos;
smooth out vec4 vertColour;
smooth out vec2 vertUv;
smooth out float vertTexId;

layout(std140) uniform transforms {
  mat4 camera;
  mat4 projection;
};

layout(std140) uniform transforms2 {
  mat4 model;
};

void main() {
  vec4 pos = vec4(inPos, 1.0);

  gl_Position = projection * camera * model * pos;
  vertPos = vec3(model * pos);
  vertColour = inColour;
  vertUv = inUv;
  vertTexId = inTexId;
}
