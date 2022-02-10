package lofimodding.opensiege.formats.aspect;

import org.joml.Vector2f;
import org.joml.Vector3f;

public record AspectBcrn(AspectVersion version, int vertexIndex, Vector3f normal, int colour, Vector2f uv) {

}
