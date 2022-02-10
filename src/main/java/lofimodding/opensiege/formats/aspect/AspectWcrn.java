package lofimodding.opensiege.formats.aspect;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4i;

public record AspectWcrn(AspectVersion version, Vector3f pos, Vector4f weight, Vector4i bone, Vector3f normal, int colour, Vector2f uv) {

}
