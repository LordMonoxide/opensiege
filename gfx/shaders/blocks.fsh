#version 330 core

smooth in vec3 vertPos;
smooth in vec3 vertNormal;
smooth in vec3 vertColour;

out vec4 outColour;

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

vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 viewDir);

void main() {
  vec3 viewDir = normalize(viewPos.xyz - vertPos);

  outColour = vec4(calculateDirectionalLight(directionalLight, vertNormal, viewDir), 1.0);
//  outColour = vec4(vertColour, 1.0);
}

vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 viewDir) {
  vec3 lightDir = normalize(-light.direction);

  // Ambient
  vec3 ambient = light.ambient * vertColour;

  // Diffuse
  float diff = max(dot(normal, lightDir), 0.0);
  vec3 diffuse = light.diffuse * diff * vertColour;

  // Specular
  vec3 halfwayDir = normalize(lightDir + viewDir);

  float spec = pow(max(dot(normal, halfwayDir), 0.0), 1.0);
  vec3 specular = light.specular * spec * vec3(0.5); //TODO texture(material.specular, vertUv).rgb;

  // Output
  return ambient + diffuse;// + specular;
}
