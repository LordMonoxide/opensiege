package lofimodding.opensiege.formats.tank;

public enum TankPriority {
  FACTORY(0x0000),
  LANGUAGE(0x1000),
  EXPANSION(0x2000),
  PATCH(0x3000),
  USER(0x4000),
  ;

  static TankPriority fromCode(final int code) {
    for(final TankPriority priority : TankPriority.values()) {
      if(priority.code == code) {
        return priority;
      }
    }

    throw new RuntimeException("Invalid code " + Integer.toHexString(code));
  }

  public final int code;

  TankPriority(final int code) {
    this.code = code;
  }
}
