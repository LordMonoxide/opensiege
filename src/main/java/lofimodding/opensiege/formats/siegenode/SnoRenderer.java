package lofimodding.opensiege.formats.siegenode;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lofimodding.opensiege.gfx.Mesh;
import org.joml.Vector3i;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;

public class SnoRenderer {
  public final Mesh mesh;
  public final Object2IntMap<String> textureIndices = new Object2IntOpenHashMap<>();

  public SnoRenderer(final Sno sno) {
    int vertexCount = 0;

    for(final SnoSurface surface : sno.surfaces()) {
      vertexCount += surface.cornerCount();
    }

    final float[] vertices = new float[vertexCount * 13];
    int vertexIndex = 0;

    for(final SnoSurface surface : sno.surfaces()) {
      for(final Vector3i face : surface.faces()) {
        for(int i = 0; i < 3; i++) {
          final SnoCorner vertex = sno.corners().get(face.get(i));

          vertices[vertexIndex++] = vertex.pos().x;
          vertices[vertexIndex++] = vertex.pos().y;
          vertices[vertexIndex++] = vertex.pos().z;
          vertices[vertexIndex++] = vertex.normal().x;
          vertices[vertexIndex++] = vertex.normal().y;
          vertices[vertexIndex++] = vertex.normal().z;
          vertices[vertexIndex++] = vertex.colour().x / 255.0f;
          vertices[vertexIndex++] = vertex.colour().y / 255.0f;
          vertices[vertexIndex++] = vertex.colour().z / 255.0f;
          vertices[vertexIndex++] = vertex.colour().w / 255.0f;
          vertices[vertexIndex++] = vertex.uv().x;
          vertices[vertexIndex++] = vertex.uv().y;
          vertices[vertexIndex++] = this.textureIndices.computeIfAbsent(surface.texture(), key -> this.textureIndices.size());
        }
      }
    }

    if(this.textureIndices.size() > 16) {
      throw new RuntimeException("Current implementation only supports 16 textures per mesh");
    }

    this.mesh = new Mesh(GL_TRIANGLES, vertices, vertexCount);
    this.mesh.attribute(0,  0, 3, 13);
    this.mesh.attribute(1,  3, 3, 13);
    this.mesh.attribute(2,  6, 4, 13);
    this.mesh.attribute(3, 10, 2, 13);
    this.mesh.attribute(4, 12, 1, 13);
  }
}
