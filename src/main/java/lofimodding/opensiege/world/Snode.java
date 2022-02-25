package lofimodding.opensiege.world;

import lofimodding.opensiege.go.GameObject;
import lofimodding.opensiege.go.GoType;
import lofimodding.opensiege.go.GoValue;

import java.util.ArrayList;
import java.util.List;

@GoType(type = "snode")
public class Snode extends GameObject {
  @GoValue(name = "bounds_camera")
  private boolean boundsCamera;

  @GoValue(name = "camera_fade")
  private boolean cameraFade;

  @GoValue
  private int guid;

  @GoValue(name = "mesh_guid")
  private int meshGuid;

  @GoValue(name = "nodelevel")
  private int nodeLevel;

  @GoValue(name = "nodeobject")
  private int nodeObject;

  @GoValue(name = "nodesection")
  private int nodeSection;

  @GoValue(name = "occludes_camera")
  private boolean occludesCamera;

  @GoValue(name = "occludes_light")
  private boolean occludesLight;

  @GoValue(name = "texsetabbr")
  private String texSetAbbr;

  @GoValue
  private final List<Door> door = new ArrayList<>();

  public boolean boundsCamera() {
    return this.boundsCamera;
  }

  public boolean cameraFade() {
    return this.cameraFade;
  }

  public int getGuid() {
    return this.guid;
  }

  public int getMeshGuid() {
    return this.meshGuid;
  }

  public int getNodeLevel() {
    return this.nodeLevel;
  }

  public int getNodeObject() {
    return this.nodeObject;
  }

  public int getNodeSection() {
    return this.nodeSection;
  }

  public boolean occludesCamera() {
    return this.occludesCamera;
  }

  public boolean occludesLight() {
    return this.occludesLight;
  }

  public String getTexSetAbbr() {
    return this.texSetAbbr;
  }

  public List<Door> getDoors() {
    return this.door;
  }

  public Door getDoor(final int id) {
    for(final Door door : this.door) {
      if(door.id == id) {
        return door;
      }
    }

    throw new RuntimeException("Failed to find door " + id);
  }

  public static class Door {
    @GoValue(name = "fardoor")
    private int farDoor;

    @GoValue(name = "farguid")
    private int farGuid;

    @GoValue
    private int id;

    public int getFarDoor() {
      return this.farDoor;
    }

    public int getFarGuid() {
      return this.farGuid;
    }

    public int getId() {
      return this.id;
    }
  }
}
