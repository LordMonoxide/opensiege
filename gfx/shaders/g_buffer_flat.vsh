#version 330 core

layout(location = 0) in vec3 inPos;
layout(location = 1) in vec3 inNormal;
layout(location = 2) in vec3 inColour;

smooth out vec3 vertPos;
smooth out vec3 vertNormal;
smooth out vec3 vertColour;

layout(std140) uniform transforms {
  mat4 camera;
  mat4 projection;
};

layout(std140) uniform transforms2 {
  mat4 model;
  mat4 modelInvTrans;
};

void main() {
  vec4 viewPos = camera * model * vec4(inPos, 1.0);

  gl_Position = projection * viewPos;
  vertPos = viewPos.xyz;
  vertNormal = normalize(transpose(inverse(mat3(camera * model))) * inNormal);
  vertColour = inColour;
}

//TODO: optimize! invert/transpose on CPU
