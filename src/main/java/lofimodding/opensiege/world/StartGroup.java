package lofimodding.opensiege.world;

import lofimodding.opensiege.go.GameObject;
import lofimodding.opensiege.go.GoType;
import lofimodding.opensiege.go.GoValue;

import java.util.List;

@GoType(type = "start_group")
public class StartGroup extends GameObject {
  @GoValue(name = "default")
  private boolean defaultStart;

  @GoValue()
  private String description;

  @GoValue(name = "dev_only")
  private boolean devOnly;

  @GoValue
  private int id;

  @GoValue(name = "screen_name")
  private String screenName;

  @GoValue(name = "start_position")
  private List<StartPosition> startPositions;

  public boolean isDefaultStart() {
    return this.defaultStart;
  }

  public String getDescription() {
    return this.description;
  }

  public boolean isDevOnly() {
    return this.devOnly;
  }

  public int getId() {
    return this.id;
  }

  public String getScreenName() {
    return this.screenName;
  }

  public List<StartPosition> getStartPositions() {
    return this.startPositions;
  }

  public static class StartPosition {
    @GoValue
    private int id;

    @GoValue
    private WorldPos position;

    @GoValue
    private Camera camera;

    public int getId() {
      return this.id;
    }

    public WorldPos getPosition() {
      return this.position;
    }

    public Camera getCamera() {
      return this.camera;
    }
  }

  public static class Camera {
    @GoValue
    private float azimuth;

    @GoValue
    private float distance;

    @GoValue
    private float orbit;

    @GoValue
    private WorldPos position;

    public float getAzimuth() {
      return this.azimuth;
    }

    public float getDistance() {
      return this.distance;
    }

    public float getOrbit() {
      return this.orbit;
    }

    public WorldPos getPosition() {
      return this.position;
    }
  }
}
