package lofimodding.opensiege.formats.aspect;

public enum AspectVersion {
  V1_2(0x201, 12),
  V1_3(0x301, 13),
  V2_0(0x002, 20),
  V2_1(0x102, 21),
  V2_2(0x202, 22),
  V2_3(0x302, 23),
  V2_4(0x402, 24),
  V2_5(0x502, 25),
  V4_0(0x004, 40),
  V4_1(0x104, 41),
  V5_0(0x005, 50),
  ;

  public static AspectVersion fromNum(final int num) {
    for(final AspectVersion version : AspectVersion.values()) {
      if(version.num == num) {
        return version;
      }
    }

    throw new RuntimeException("Invalid num " + Integer.toHexString(num));
  }

  public final int num;
  public final int normalized;

  AspectVersion(final int num, final int normalized) {
    this.num = num;
    this.normalized = normalized;
  }
}
