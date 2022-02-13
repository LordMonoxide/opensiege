package lofimodding.opensiege.world;

import org.joml.Vector3f;

public class WorldPos {
  private final long nodeId;
  private final Vector3f pos = new Vector3f();

  public WorldPos(final long nodeId, final float x, final float y, final float z) {
    this.nodeId = nodeId;
    this.pos.set(x, y, z);
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
    return "WorldPos[" + Long.toHexString(this.nodeId) + ", (" + this.pos.x + ", " + this.pos.y + ", " + this.pos.z + ")]";
  }
}
