#version 330 core

layout(location = 0) in vec3 inPos;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec2 inUv;

smooth out vec3 vertPos;
smooth out vec3 vertNormal;
smooth out vec2 vertUv;

layout(std140) uniform transforms {
  mat4 camera;
  mat4 projection;
};

layout(std140) uniform transforms2 {
  mat4 model;
  mat4 modelInvTrans;
};

void main() {
  vec4 pos = vec4(inPos, 1.0);

  gl_Position = projection * camera * model * pos;
  vertPos = vec3(model * pos);
  vertNormal = normalize(mat3(modelInvTrans) * inNormal);
  vertUv = inUv;
}
