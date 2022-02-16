package lofimodding.opensiege.world;

import lofimodding.opensiege.go.GameObject;
import lofimodding.opensiege.go.GoId;
import lofimodding.opensiege.go.GoType;
import lofimodding.opensiege.go.GoValue;

import java.time.LocalTime;

@GoType(type = "map", loader = MapLoader.class)
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
}
