package lofimodding.opensiege.world;

import org.joml.Vector3f;

public class WorldPos {
  private final int nodeId;
  private final Vector3f pos = new Vector3f();

  public WorldPos(final int nodeId, final float x, final float y, final float z) {
    this.nodeId = nodeId;
    this.pos.set(x, y, z);
  }

  public int getNodeId() {
    return this.nodeId;
  }

  public float getX() {
    return this.pos.x;
  }

  public float getY() {
    return this.pos.y;
  }

  public float getZ() {
    return this.pos.z;
  }

  @Override
  public boolean equals(final Object obj) {
    if(obj instanceof final WorldPos other) {
      return this.nodeId == other.nodeId && this.pos.equals(other.pos);
    }

    return false;
  }

  @Override
  public String toString() {
    return "WorldPos[" + Integer.toHexString(this.nodeId) + ", (" + this.pos.x + ", " + this.pos.y + ", " + this.pos.z + ")]";
  }
}
