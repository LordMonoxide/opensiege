package lofimodding.opensiege.formats.siegenode;

import org.joml.Matrix3f;
import org.joml.Vector3f;

public record SnoSpot(Matrix3f rotation, Vector3f translation, String name) {

}
