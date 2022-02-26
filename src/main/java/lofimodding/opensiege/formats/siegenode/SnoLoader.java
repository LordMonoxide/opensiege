package lofimodding.opensiege.formats.siegenode;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.joml.Matrix3f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static lofimodding.opensiege.formats.StreamReader.read4cc;
import static lofimodding.opensiege.formats.StreamReader.readCString;
import static lofimodding.opensiege.formats.StreamReader.readInt;
import static lofimodding.opensiege.formats.StreamReader.readMat3;
import static lofimodding.opensiege.formats.StreamReader.readVec2;
import static lofimodding.opensiege.formats.StreamReader.readVec3;
import static lofimodding.opensiege.formats.StreamReader.readVec3s;
import static lofimodding.opensiege.formats.StreamReader.readVec4b;

public final class SnoLoader {
  private SnoLoader() { }

  public static Sno load(final InputStream file, final String texSet) throws IOException {
    final SnoHeader header = readHeader(file);
    final Int2ObjectMap<SnoDoor> doors = readDoors(file, header.doorCount());
    final List<SnoSpot> spots = readSpots(file, header.spotCount());
    final List<SnoCorner> corners = readCorners(file, header.cornerCount());
    final List<SnoSurface> surfaces = readSurfaces(file, header.textureCount(), texSet);

    return new Sno(header, spots, doors, corners, surfaces);
  }

  private static SnoHeader readHeader(final InputStream file) throws IOException {
    final String magic = read4cc(file);

    if(!"SNOD".equals(magic)) {
      throw new IOException("Invalid sno model - bad magic");
    }

    final int version = readInt(file);
    readInt(file); // unused
    final int doorCount = readInt(file);
    final int spotCount = readInt(file);
    final int cornerCount = readInt(file);
    final int faceCount = readInt(file);
    final int textureCount = readInt(file);
    final Vector3f minBb = readVec3(file);
    final Vector3f maxBb = readVec3(file);

    for(int i = 0; i < 7; i++) {
      readInt(file); // unused
    }

    final int dataCrc = readInt(file);

    return new SnoHeader(version, doorCount, spotCount, cornerCount, faceCount, textureCount, minBb, maxBb, dataCrc);
  }

  private static Int2ObjectMap<SnoDoor> readDoors(final InputStream file, final int count) throws IOException {
    final Int2ObjectMap<SnoDoor> doors = new Int2ObjectOpenHashMap<>(count);

    for(int i = 0; i < count; i++) {
      final int index = readInt(file);
      final Vector3f translation = readVec3(file);
      final Matrix3f rotation = readMat3(file);

      final IntList hotspots = new IntArrayList();
      final int hotspotCount = readInt(file);
      for(int hotspotIndex = 0; hotspotIndex < hotspotCount; hotspotIndex++) {
        hotspots.add(readInt(file));
      }

      doors.put(index, new SnoDoor(index, rotation, translation, hotspots));
    }

    return doors;
  }

  private static List<SnoSpot> readSpots(final InputStream file, final int count) throws IOException {
    final List<SnoSpot> spots = new ArrayList<>(count);

    for(int i = 0; i < count; i++) {
      final Matrix3f rotation = readMat3(file);
      final Vector3f translation = readVec3(file);
      final String name = readCString(file);

      spots.add(new SnoSpot(rotation, translation, name));
    }

    return spots;
  }

  private static List<SnoCorner> readCorners(final InputStream file, final int count) throws IOException {
    final List<SnoCorner> corners = new ArrayList<>(count);

    for(int i = 0; i < count; i++) {
      final Vector3f position = readVec3(file);
      final Vector3f normal = readVec3(file);

      // Stored in RGBA format...
      final Vector4i whackColour = readVec4b(file);
      final Vector4i colour = new Vector4i(whackColour.x, whackColour.z, whackColour.y, whackColour.w);

      final Vector2f uv = readVec2(file);

      corners.add(new SnoCorner(position, normal, colour, uv));
    }

    return corners;
  }

  private static List<SnoSurface> readSurfaces(final InputStream file, final int count, final String texSet) throws IOException {
    final List<SnoSurface> surfaces = new ArrayList<>(count);

    for(int i = 0; i < count; i++) {
      final String texture = readCString(file).replace("_xxx_", '_' + texSet + '_');
      final int cornerStart = readInt(file);
      final int cornerSpan = readInt(file);
      final int cornerCount = readInt(file);

      final int faceCount = cornerCount / 3;
      final List<Vector3i> faces = new ArrayList<>(faceCount);

      for(int faceIndex = 0; faceIndex < faceCount; faceIndex++) {
        faces.add(readVec3s(file));
      }

      surfaces.add(new SnoSurface(cornerStart, cornerSpan, cornerCount, faces, texture));
    }

    return surfaces;
  }
}
