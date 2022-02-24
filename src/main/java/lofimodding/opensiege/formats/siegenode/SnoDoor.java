package lofimodding.opensiege.formats.siegenode;

import it.unimi.dsi.fastutil.ints.IntList;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public record SnoDoor(int index, Matrix3f rotation, Vector3f translation, IntList hotspots) {

}
