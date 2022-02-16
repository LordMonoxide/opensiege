package lofimodding.opensiege.world;

import lofimodding.opensiege.go.GameObject;
import lofimodding.opensiege.go.GoId;
import lofimodding.opensiege.go.GoType;
import lofimodding.opensiege.go.GoValue;

import java.time.LocalTime;

@GoType(type = "map")
public class Map extends GameObject {
  @GoValue
  private String description;
  @GoValue
  private GoId name;
  @GoValue(name = "screen_name")
  private String screenName;
  @GoValue(name = "world_frustum_radius")
  private int worldFrustumRadius;
  @GoValue(name = "world_interest_radius")
  private int worldInterestRadius;
  @GoValue
  private String notes;
  @GoValue(name = "start_region_name")
  private GoId startRegionName;
  @GoValue(name = "show_intro")
  private boolean showIntro;
  @GoValue(name = "timeofday")
  private LocalTime timeOfDay;
  @GoValue
  private Camera camera;
  @GoValue
  private Worlds worlds;

  public static class Camera {
    @GoValue
    private float azimuth;
    @GoValue
    private float distance;
    @GoValue(name = "farclip")
    private float farClip;
    @GoValue
    private float fov;
    @GoValue(name = "max_azimuth")
    private float maxAzimuth;
    @GoValue(name = "max_distance")
    private float maxDistance;
    @GoValue(name = "min_azimuth")
    private float minAzimuth;
    @GoValue(name = "min_distance")
    private float minDistance;
    @GoValue(name = "nearclip")
    private float nearClip;
    @GoValue
    private float orbit;
    @GoValue
    private WorldPos position;
  }

  public static class Worlds {
    @GoValue
    private Normal normal;
  }

  public static class Normal {
    @GoValue(name = "screen_name")
    private String screenName;
    @GoValue
    private String description;
    @GoValue(name = "required_level")
    private int requiredLevel;
  }
}
