package lofimodding.opensiege.formats.siegenode;

import org.joml.Vector3f;

public record SnoHeader(int version, int doorCount, int spotCount, int cornerCount, int faceCount, int textureCount, Vector3f minBb, Vector3f maxBb, int dataCrc) {

}
