#version 330 core

smooth in vec3 vertPos;
smooth in vec3 vertNormal;
smooth in vec2 vertUv;

out vec4 outColour;

struct Material {
  sampler2D diffuse;
  vec3 specular;
  float shininess;
};

struct DirectionalLight {
  vec3 direction;

  vec3 ambient;
  vec3 diffuse;
  vec3 specular;
};

uniform Material material;
uniform DirectionalLight directionalLight;

layout(std140) uniform lighting {
// vec3, but OpenGL uses 4-byte memory boundaries
  vec4 viewPos;
};

vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 viewDir);

void main() {
  vec3 viewDir = normalize(viewPos.xyz - vertPos);

  outColour = vec4(calculateDirectionalLight(directionalLight, vertNormal, viewDir), 1.0);
//  outColour = texture(material.diffuse, vertUv);
}

vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 viewDir) {
  vec3 lightDir = normalize(-light.direction);

  // Ambient
  vec3 ambient = light.ambient * texture(material.diffuse, vertUv).rgb;

  // Diffuse
  float diff = max(dot(normal, lightDir), 0.0);
  vec3 diffuse = light.diffuse * diff * texture(material.diffuse, vertUv).rgb;

  // Specular
  vec3 halfwayDir = normalize(lightDir + viewDir);

  float spec = pow(max(dot(normal, halfwayDir), 0.0), material.shininess);
  vec3 specular = light.specular * spec * material.specular; //TODO texture(material.specular, vertUv).rgb;

  // Output
  return ambient + diffuse;// + specular;
}
