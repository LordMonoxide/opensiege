package lofimodding.opensiege.formats.aspect;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public record AspectRpos(AspectVersion version, Quaternionf[] rotation, Vector3f[] pos) {

}
