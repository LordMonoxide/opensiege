package lofimodding.opensiege.formats.raw;

import java.io.IOException;
import java.io.InputStream;

import static lofimodding.opensiege.formats.StreamReader.read4cc;
import static lofimodding.opensiege.formats.StreamReader.readBytes;
import static lofimodding.opensiege.formats.StreamReader.readInt16;

public final class RawTextureLoader {
  private RawTextureLoader() { }

  public static RawTexture load(final InputStream file) throws IOException {
    //TODO gas file?

    final RawTextureHeader header = readHeader(file);
    final byte[] data = readBytes(file, header.width() * header.height() * header.format().bpp / 8);

    return new RawTexture(header, data);
  }

  private static RawTextureHeader readHeader(final InputStream file) throws IOException {
    final String magic = read4cc(file);

    if(!"ipaR".equals(magic)) {
      throw new IOException("Invalid raw texture - bad magic");
    }

    final RawTextureFormat format = RawTextureFormat.fromMagic(read4cc(file));

    final int flags = readInt16(file);

    if(flags != 0) {
      throw new IOException("Invalid raw texture - bad flags");
    }

    final int surfaceCount = readInt16(file);

    if(surfaceCount < 1) {
      throw new IOException("Invalid raw texture - must have at least 1 surface");
    }

    final int width = readInt16(file);
    final int height = readInt16(file);

    if(width < 1 || height < 1) {
      throw new IOException("Invalid raw texture - width and height must be at least 1");
    }

    return new RawTextureHeader(format, flags, surfaceCount, width, height);
  }
}
