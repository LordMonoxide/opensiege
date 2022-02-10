package lofimodding.opensiege.formats.tank;

public enum TankFlags {
  NONE(0x0),
  NON_RETAIL(0x1),
  ALLOW_MULTIPLAYER_XFER(0x2),
  PROTECTED_CONTENT(0x4),
  ;

  static TankFlags fromCode(final int code) {
    for(final TankFlags flags : TankFlags.values()) {
      if(flags.code == code) {
        return flags;
      }
    }

    throw new RuntimeException("Invalid code " + Integer.toHexString(code));
  }

  public final int code;

  TankFlags(final int code) {
    this.code = code;
  }
}
