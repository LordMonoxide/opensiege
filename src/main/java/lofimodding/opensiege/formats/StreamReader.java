package lofimodding.opensiege.formats;

import org.joml.Matrix3f;
import org.joml.Matrix4x3f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public final class StreamReader {
  private StreamReader() { }

  /**
   * Does not exception if it doesn't read enough data
   */
  public static String read4cc(final InputStream file) throws IOException {
    final byte[] raw = new byte[4];

    if(file.read(raw) < 4) {
      return "";
    }

    return new String(raw);
  }

  public static String readString(final InputStream file, final int length) throws IOException {
    return new String(readBytes(file, length));
  }

  public static String readNString(final InputStream file) throws IOException {
    final int length = readInt16(file);
    final String string = readString(file, length);

    // 4-byte align
    file.skip(4 - (length + 2) % 4);

    return string;
  }

  public static String readCString(final InputStream file) throws IOException {
    final StringBuilder sb = new StringBuilder();

    while(true) {
      final char c = (char)file.read();
      if(c == 0) {
        break;
      }
      sb.append(c);
    }

    return sb.toString();
  }

  public static int readInt16(final InputStream file) throws IOException {
    return readInt16(readBytes(file, 2), 0);
  }

  public static int readInt16(final byte[] data, final int offset) throws IOException {
    return (data[offset + 1] & 0xff) << 8 | data[offset] & 0xff;
  }

  public static int readInt(final InputStream file) throws IOException {
    return readInt(readBytes(file, 4), 0);
  }

  public static int readInt(final byte[] data, final int offset) {
    return (data[offset + 3] & 0xff) << 24 | (data[offset + 2] & 0xff) << 16 | (data[offset + 1] & 0xff) << 8 | data[offset] & 0xff;
  }

  public static long readLong(final InputStream file) throws IOException {
    return readLong(readBytes(file, 8), 0);
  }

  public static long readLong(final byte[] data, final int offset) {
    return (data[offset + 7] & 0xffL) << 56 | (data[offset + 6] & 0xffL) << 48 | (data[offset + 5] & 0xffL) << 40 | (data[offset + 4] & 0xffL) << 32 | (data[offset + 3] & 0xffL) << 24 | (data[offset + 2] & 0xffL) << 16 | (data[offset + 1] & 0xffL) << 8 | data[offset] & 0xffL;
  }

  public static float readFloat(final InputStream file) throws IOException {
    return Float.intBitsToFloat(readInt(file));
  }

  public static byte[] readBytes(final InputStream file, final int length) throws IOException {
    final byte[] data = new byte[length];

    if(file.read(data) < length) {
      throw new EOFException("End of file reached");
    }

    return data;
  }

  public static Vector2f readVec2(final InputStream file) throws IOException {
    return new Vector2f(readFloat(file), readFloat(file));
  }

  public static Vector3f readVec3(final InputStream file) throws IOException {
    return new Vector3f(readFloat(file), readFloat(file), readFloat(file));
  }

  public static Vector3i readVec3i(final InputStream file) throws IOException {
    return new Vector3i(readInt(file), readInt(file), readInt(file));
  }

  public static Vector3i readVec3s(final InputStream file) throws IOException {
    return new Vector3i(readInt16(file), readInt16(file), readInt16(file));
  }

  public static Vector4f readVec4(final InputStream file) throws IOException {
    return new Vector4f(readFloat(file), readFloat(file), readFloat(file), readFloat(file));
  }

  public static Vector4i readVec4b(final InputStream file) throws IOException {
    return new Vector4i(file.read(), file.read(), file.read(), file.read());
  }

  public static Quaternionf readQuat(final InputStream file) throws IOException {
    return new Quaternionf(readFloat(file), readFloat(file), readFloat(file), readFloat(file));
  }

  public static Matrix3f readMat3(final InputStream file) throws IOException {
    return new Matrix3f(readFloat(file), readFloat(file), readFloat(file), readFloat(file), readFloat(file), readFloat(file), readFloat(file), readFloat(file), readFloat(file));
  }
}
