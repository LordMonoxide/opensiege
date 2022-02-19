package lofimodding.opensiege.world;

import lofimodding.opensiege.go.GameObject;
import lofimodding.opensiege.go.GoType;
import lofimodding.opensiege.go.GoValue;

@GoType(type = "region")
public class Region extends GameObject {
  @GoValue
  private String created;
  @GoValue
  private String description;
  @GoValue
  private int guid;
  @GoValue(name = "lastmodified")
  private String lastModified;
  @GoValue
  private String notes;
  @GoValue(name = "scid_range")
  private int scidRange;
  @GoValue(name = "se_version")
  private String seVersion;
}
