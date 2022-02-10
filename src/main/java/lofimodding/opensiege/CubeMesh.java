package lofimodding.opensiege;

import lofimodding.opensiege.gfx.Mesh;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;

public class CubeMesh {
  private static final float[] VERTICES = {
    // Back
    -0.5f, -0.5f, -0.5f,   0.0f,  0.0f, -1.0f,   0.0f, 1.0f, // Bottom-left
     0.5f,  0.5f, -0.5f,   0.0f,  0.0f, -1.0f,   1.0f, 0.0f, // top-right
     0.5f, -0.5f, -0.5f,   0.0f,  0.0f, -1.0f,   1.0f, 1.0f, // bottom-right
     0.5f,  0.5f, -0.5f,   0.0f,  0.0f, -1.0f,   1.0f, 0.0f, // top-right
    -0.5f, -0.5f, -0.5f,   0.0f,  0.0f, -1.0f,   0.0f, 1.0f, // Bottom-left
    -0.5f,  0.5f, -0.5f,   0.0f,  0.0f, -1.0f,   0.0f, 0.0f, // top-left

    // Front
    -0.5f, -0.5f,  0.5f,   0.0f,  0.0f,  1.0f,   0.0f, 1.0f, // bottom-left
     0.5f, -0.5f,  0.5f,   0.0f,  0.0f,  1.0f,   1.0f, 1.0f, // bottom-right
     0.5f,  0.5f,  0.5f,   0.0f,  0.0f,  1.0f,   1.0f, 0.0f, // top-right
     0.5f,  0.5f,  0.5f,   0.0f,  0.0f,  1.0f,   1.0f, 0.0f, // top-right
    -0.5f,  0.5f,  0.5f,   0.0f,  0.0f,  1.0f,   0.0f, 0.0f, // top-left
    -0.5f, -0.5f,  0.5f,   0.0f,  0.0f,  1.0f,   0.0f, 1.0f, // bottom-left

    // Left
    -0.5f,  0.5f,  0.5f,  -1.0f,  0.0f,  0.0f,   1.0f, 0.0f, // top-right
    -0.5f,  0.5f, -0.5f,  -1.0f,  0.0f,  0.0f,   0.0f, 0.0f, // top-left
    -0.5f, -0.5f, -0.5f,  -1.0f,  0.0f,  0.0f,   0.0f, 1.0f, // bottom-left
    -0.5f, -0.5f, -0.5f,  -1.0f,  0.0f,  0.0f,   0.0f, 1.0f, // bottom-left
    -0.5f, -0.5f,  0.5f,  -1.0f,  0.0f,  0.0f,   1.0f, 1.0f, // bottom-right
    -0.5f,  0.5f,  0.5f,  -1.0f,  0.0f,  0.0f,   1.0f, 0.0f, // top-right

    // Right
     0.5f,  0.5f,  0.5f,   1.0f,  0.0f,  0.0f,   0.0f, 0.0f, // top-left
     0.5f, -0.5f, -0.5f,   1.0f,  0.0f,  0.0f,   1.0f, 1.0f, // bottom-right
     0.5f,  0.5f, -0.5f,   1.0f,  0.0f,  0.0f,   1.0f, 0.0f, // top-right
     0.5f, -0.5f, -0.5f,   1.0f,  0.0f,  0.0f,   1.0f, 1.0f, // bottom-right
     0.5f,  0.5f,  0.5f,   1.0f,  0.0f,  0.0f,   0.0f, 0.0f, // top-left
     0.5f, -0.5f,  0.5f,   1.0f,  0.0f,  0.0f,   0.0f, 1.0f, // bottom-left

    // Bottom
    -0.5f, -0.5f, -0.5f,   0.0f, -1.0f,  0.0f,   0.0f, 0.0f, // top-right
     0.5f, -0.5f, -0.5f,   0.0f, -1.0f,  0.0f,   1.0f, 0.0f, // top-left
     0.5f, -0.5f,  0.5f,   0.0f, -1.0f,  0.0f,   1.0f, 1.0f, // bottom-left
     0.5f, -0.5f,  0.5f,   0.0f, -1.0f,  0.0f,   1.0f, 1.0f, // bottom-left
    -0.5f, -0.5f,  0.5f,   0.0f, -1.0f,  0.0f,   0.0f, 1.0f, // bottom-right
    -0.5f, -0.5f, -0.5f,   0.0f, -1.0f,  0.0f,   0.0f, 0.0f, // top-right

    // Top
    -0.5f,  0.5f, -0.5f,   0.0f,  1.0f,  0.0f,   0.0f, 0.0f, // top-left
     0.5f,  0.5f,  0.5f,   0.0f,  1.0f,  0.0f,   1.0f, 1.0f, // bottom-right
     0.5f,  0.5f, -0.5f,   0.0f,  1.0f,  0.0f,   1.0f, 0.0f, // top-right
     0.5f,  0.5f,  0.5f,   0.0f,  1.0f,  0.0f,   1.0f, 1.0f, // bottom-right
    -0.5f,  0.5f, -0.5f,   0.0f,  1.0f,  0.0f,   0.0f, 0.0f, // top-left
    -0.5f,  0.5f,  0.5f,   0.0f,  1.0f,  0.0f,   0.0f, 1.0f  // bottom-left
  };

  private final Mesh mesh;

  public CubeMesh() {
    this.mesh = new Mesh(GL_TRIANGLES, VERTICES, 36);
    this.mesh.attribute(0, 0L, 3, 8);
    this.mesh.attribute(1, 3L, 3, 8);
    this.mesh.attribute(2, 6L, 2, 8);
  }

  public void draw() {
    this.mesh.draw();
  }
}
