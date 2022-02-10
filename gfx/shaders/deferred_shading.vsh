#version 330 core

layout(location = 0) in vec3 inPos;
layout(location = 1) in vec2 inUv;

out vec2 vertUv;

void main() {
  vertUv = inUv;
  gl_Position = vec4(inPos, 1.0);
}
