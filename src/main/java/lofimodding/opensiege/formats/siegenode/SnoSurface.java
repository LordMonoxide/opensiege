package lofimodding.opensiege.formats.siegenode;

import org.joml.Vector3i;

import java.util.List;

public record SnoSurface(int cornerStart, int cornerSpan, int cornerCount, List<Vector3i> faces, String texture) {

}
