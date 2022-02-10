package lofimodding.opensiege.formats.aspect;

import org.joml.Vector3i;

public record AspectBtri(AspectVersion version, int[] cornerStart, int[] cornerSpan, Vector3i[] cornerIndex) {

}
