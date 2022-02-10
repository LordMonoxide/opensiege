#version 330 core

smooth in vec2 vertUv;

out vec4 outColour;

uniform sampler2D gPosition;
uniform sampler2D gNormal;
uniform sampler2D gColour;
uniform sampler2D ssao;

struct DirectionalLight {
  vec3 direction;

  vec3 ambient;
  vec3 diffuse;
  vec3 specular;
};

uniform DirectionalLight directionalLight;

layout(std140) uniform lighting {
// vec3, but OpenGL uses 4-byte memory boundaries
  vec4 viewPos;
};

vec3 calculateDirectionalLight(DirectionalLight light, float occlusion, vec3 normal, vec3 viewDir);

void main() {
  vec3 vertPos = texture(gPosition, vertUv).xyz;
  vec3 vertNormal = texture(gNormal, vertUv).xyz;
  vec3 viewDir = normalize(viewPos.xyz - vertPos);
  float occlusion = texture(ssao, vertUv).r;

  outColour = vec4(calculateDirectionalLight(directionalLight, occlusion, vertNormal, viewDir), 1.0);

  // Gamma correct
  float gamma = 2.2;

  outColour.rgb = pow(outColour.rgb, vec3(1.0 / gamma));
}

vec3 calculateDirectionalLight(DirectionalLight light, float occlusion, vec3 normal, vec3 viewDir) {
  vec4 colour = texture(gColour, vertUv);

  vec3 lightDir = normalize(-light.direction);

  // Ambient
  vec3 ambient = light.ambient * colour.rgb * occlusion;

  // Diffuse
  float diff = max(dot(normal, lightDir), 0.0);
  vec3 diffuse = diff * colour.rgb * light.diffuse;

  // Specular
  vec3 halfwayDir = normalize(lightDir + viewDir);

  float spec = pow(max(dot(normal, halfwayDir), 0.0), 1);
  vec3 specular = light.specular * spec * colour.a; //TODO texture(material.specular, vertUv).rgb;

  // Output
  return ambient + diffuse + specular;
}
