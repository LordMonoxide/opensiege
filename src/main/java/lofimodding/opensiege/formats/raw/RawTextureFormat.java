package lofimodding.opensiege.formats.raw;

public enum RawTextureFormat {
  RGBA8("8888", 32),
  ;

  public static RawTextureFormat fromMagic(final String magic) {
    for(final RawTextureFormat format : values()) {
      if(format.magic.equals(magic)) {
        return format;
      }
    }

    throw new RuntimeException("Invalid magic " + magic);
  }

  public final String magic;
  public final int bpp;

  RawTextureFormat(final String magic, final int bpp) {
    this.magic = magic;
    this.bpp = bpp;
  }
}
