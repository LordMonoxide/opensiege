package lofimodding.opensiege.world;

import lofimodding.opensiege.go.GameObject;
import lofimodding.opensiege.go.GoType;
import lofimodding.opensiege.go.GoValue;

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
  private List<Door> door;

  public static class Door {
    @GoValue(name = "fardoor")
    private int farDoor;

    @GoValue(name = "farguid")
    private int farGuid;

    @GoValue
    private int id;
  }
}
