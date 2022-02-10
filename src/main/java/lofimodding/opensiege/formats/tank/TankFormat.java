package lofimodding.opensiege.formats.tank;

public enum TankFormat {
  RAW,
  ZLIB,
  LZO,
  ;

  static TankFormat fromIndex(final int index) {
    return TankFormat.values()[index];
  }

  public boolean isCompressed() {
    return this != RAW;
  }
}
