package lofimodding.opensiege.gfx.lights;

import org.joml.Vector3f;
import lofimodding.opensiege.gfx.Shader;

public class DirectionalLight {
  public final Vector3f direction;

  public final Vector3f ambient;
  public final Vector3f diffuse;
  public final Vector3f specular;

  public DirectionalLight(final Vector3f direction, final Vector3f ambient, final Vector3f diffuse, final Vector3f specular) {
    this.direction = direction;

    this.ambient  = ambient;
    this.diffuse  = diffuse;
    this.specular = specular;
  }

  public void use(final Shader.UniformVec3 direction, final Shader.UniformVec3 ambient, final Shader.UniformVec3 diffuse, final Shader.UniformVec3 specular) {
    direction.set(this.direction);
    ambient.set(this.ambient);
    diffuse.set(this.diffuse);
    specular.set(this.specular);
  }
}
