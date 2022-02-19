package lofimodding.opensiege.world;

import lofimodding.opensiege.go.GameObject;
import lofimodding.opensiege.go.GoType;
import lofimodding.opensiege.go.GoValue;

@GoType(type = "terrain_nodes")
public class TerrainNodes extends GameObject {
  @GoValue(name = "actor_ambient_color")
  private int actorAmbientColor;

  @GoValue(name = "actor_ambient_intensity")
  private float actorAmbientIntensity;

  @GoValue
  private float ambience;

  @GoValue(name = "ambient_color")
  private int ambientColor;

  @GoValue(name = "ambient_intensity")
  private float ambientIntensity;

  @GoValue(name = "environment_map")
  private String environmentMap;

  @GoValue(name = "object_ambient_color")
  private int objectAmbientColor;

  @GoValue(name = "object_ambient_intensity")
  private float objectAmbientIntensity;

  @GoValue(name = "targetnode")
  private int targetNode;

  @GoValue
  private String type;

  //TODO the map of snodes...?
}
