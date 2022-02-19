package lofimodding.opensiege.world;

import lofimodding.opensiege.go.GameObject;
import lofimodding.opensiege.go.GoType;
import lofimodding.opensiege.go.GoValue;

import java.time.LocalTime;

@GoType(type = "map")
public class World extends GameObject {
  @GoValue
  private String name;

  @GoValue
  private String description;

  @GoValue(name = "screen_name")
  private String screenName;

  @GoValue(name = "world_frustum_radius")
  private float worldFrustumRadius;

  @GoValue(name = "world_interest_radius")
  private float worldInterestRadius;

  @GoValue
  private String notes;

  @GoValue(name = "start_region_name")
  private String startRegionName;

  @GoValue(name = "show_intro")
  private boolean showIntro;

  @GoValue(name = "timeofday")
  private LocalTime timeOfDay;

  @GoValue
  private Camera camera;

  @GoValue
  private Worlds worlds;

  public String getDescription() {
    return this.description;
  }

  public String getName() {
    return this.name;
  }

  public String getScreenName() {
    return this.screenName;
  }

  public float getWorldFrustumRadius() {
    return this.worldFrustumRadius;
  }

  public float getWorldInterestRadius() {
    return this.worldInterestRadius;
  }

  public String getNotes() {
    return this.notes;
  }

  public String getStartRegionName() {
    return this.startRegionName;
  }

  public boolean isShowIntro() {
    return this.showIntro;
  }

  public LocalTime getTimeOfDay() {
    return this.timeOfDay;
  }

  public Camera getCamera() {
    return this.camera;
  }

  public Worlds getWorlds() {
    return this.worlds;
  }

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

    public float getAzimuth() {
      return this.azimuth;
    }

    public float getDistance() {
      return this.distance;
    }

    public float getFarClip() {
      return this.farClip;
    }

    public float getFov() {
      return this.fov;
    }

    public float getMaxAzimuth() {
      return this.maxAzimuth;
    }

    public float getMaxDistance() {
      return this.maxDistance;
    }

    public float getMinAzimuth() {
      return this.minAzimuth;
    }

    public float getMinDistance() {
      return this.minDistance;
    }

    public float getNearClip() {
      return this.nearClip;
    }

    public float getOrbit() {
      return this.orbit;
    }

    public WorldPos getPosition() {
      return this.position;
    }
  }

  public static class Worlds {
    @GoValue
    private Normal normal;

    public Normal getNormal() {
      return this.normal;
    }
  }

  public static class Normal {
    @GoValue(name = "screen_name")
    private String screenName;

    @GoValue
    private String description;

    @GoValue(name = "required_level")
    private int requiredLevel;

    public String getScreenName() {
      return this.screenName;
    }

    public String getDescription() {
      return this.description;
    }

    public int getRequiredLevel() {
      return this.requiredLevel;
    }
  }
}
