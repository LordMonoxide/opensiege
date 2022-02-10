package lofimodding.opensiege.formats.aspect;

import lofimodding.opensiege.gfx.Mesh;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;

public final class AspectLoader {
  private AspectLoader() { }

  public static Aspect load(final InputStream file) throws IOException {
    AspectBmsh bmsh = null;
    AspectBsub currentBsub = null;
    final List<AspectBonh> bonhs = new ArrayList<>();
    final List<AspectBsub> bsubs = new ArrayList<>();
    final Map<AspectBsub, List<AspectBsmm>> bsmms = new HashMap<>();
    final List<AspectBvtx> bvtxs = new ArrayList<>();
    final Map<AspectBsub, List<AspectBcrn>> bcrns = new HashMap<>();
    final Map<AspectBsub, List<AspectWcrn>> wcrns = new HashMap<>();
    final Map<AspectBsub, List<AspectBvmp>> bvmps = new HashMap<>();
    final Map<AspectBsub, AspectBtri> btris = new HashMap<>();
    final List<AspectBvwl> bvwls = new ArrayList<>();
    final List<AspectStch> stchs = new ArrayList<>();
    final List<AspectRpos> rposs = new ArrayList<>();

    for(String chunkId = read4cc(file); !chunkId.isEmpty(); chunkId = read4cc(file)) {
      switch(chunkId) {
        case "BMSH" -> {
          if(bmsh != null) {
            throw new IOException("Multiple bmsh definitions in " + file);
          }

          bmsh = readBmsh(file);
        }

        case "BONH" -> {
          if(bmsh == null) {
            throw new IOException("No bmsh definition");
          }

          bonhs.addAll(readBonh(file, bmsh));
        }

        case "BSUB" -> {
          currentBsub = readBsub(file);
          bsubs.add(currentBsub);
        }

        case "BSMM" -> bsmms.computeIfAbsent(currentBsub, key -> new ArrayList<>()).addAll(readBsmm(file));
        case "BVTX" -> bvtxs.addAll(readBvtx(file));
        case "BCRN" -> bcrns.computeIfAbsent(currentBsub, key -> new ArrayList<>()).addAll(readBcrn(file));
        case "WCRN" -> wcrns.computeIfAbsent(currentBsub, key -> new ArrayList<>()).addAll(readWcrn(file));
        case "BVMP" -> bvmps.computeIfAbsent(currentBsub, key -> new ArrayList<>()).addAll(readBvmp(file, currentBsub));
        case "BTRI" -> btris.put(currentBsub, readBtri(file, currentBsub));

        case "BVWL" -> {
          if(bmsh == null) {
            throw new IOException("No bmsh definition");
          }

          bvwls.addAll(readBvwl(file, bmsh));
        }

        case "STCH" -> stchs.addAll(readStch(file));
        case "RPOS" -> rposs.addAll(readRpos(file));
        case "BBOX" -> readBbox(file);
        case "BEND" -> readBend(file);

        default -> throw new RuntimeException("Not implemented " + chunkId);
      }
    }

    final List<Mesh> meshes = new ArrayList<>();

    int cornerOffset = 0;

    for(final AspectBsub bsub : bsubs) {
      final List<AspectWcrn> corners = wcrns.get(bsub);

      final float[] vertices = new float[bsub.cornerCount() * 12];
      int vertexIndex = 0;

      for(final AspectWcrn corner : corners) {
        vertices[vertexIndex++] = corner.pos().x;
        vertices[vertexIndex++] = corner.pos().y;
        vertices[vertexIndex++] = corner.pos().z;
        vertices[vertexIndex++] = corner.normal().x;
        vertices[vertexIndex++] = corner.normal().y;
        vertices[vertexIndex++] = corner.normal().z;
        vertices[vertexIndex++] = corner.colour().x / 255.0f;
        vertices[vertexIndex++] = corner.colour().y / 255.0f;
        vertices[vertexIndex++] = corner.colour().z / 255.0f;
        vertices[vertexIndex++] = corner.colour().w / 255.0f;
        vertices[vertexIndex++] = corner.uv().x;
        vertices[vertexIndex++] = corner.uv().y;

        //TODO add texture index as vertex attrib
      }

      int cornerIndexIndex = 0;

      final List<AspectBsmm> bsmm = bsmms.get(bsub);
      final AspectBtri btri = btris.get(bsub);

      final List<Vector3i> faces = new ArrayList<>();

      for(int subTextureIndex = 0; subTextureIndex < bsub.subTextures(); subTextureIndex++) {
        for(int i = 0; i < bsmm.get(subTextureIndex).faceSpan(); i++) {
          final int add = btri.cornerStart()[subTextureIndex] + cornerOffset;
          faces.add(new Vector3i(btri.cornerIndex()[cornerIndexIndex]).add(add, add, add));

          cornerIndexIndex++;
        }
      }

      cornerOffset += bsub.cornerCount();

      final int[] indices = new int[faces.size() * 3];
      int indexIndex = 0;
      for(final Vector3i face : faces) {
        indices[indexIndex++] = face.x;
        indices[indexIndex++] = face.y;
        indices[indexIndex++] = face.z;
      }

      final Mesh mesh = new Mesh(GL_TRIANGLES, vertices, indices);
      mesh.attribute(0,  0, 3, 12);
      mesh.attribute(1,  3, 3, 12);
      mesh.attribute(2,  6, 4, 12);
      mesh.attribute(3, 10, 2, 12);

      meshes.add(mesh);
    }

    return new Aspect(meshes);
  }

  private static AspectBmsh readBmsh(final InputStream file) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final int textFieldSize = readInt(file);
    final int boneCount = readInt(file);
    final int textureCount = readInt(file);
    final int vertexCount = readInt(file);
    final int submeshCount = readInt(file);
    final int renderFlags = readInt(file);

    final String[] rawText = readString(file, textFieldSize).split("\0+");

    final String[] textures = new String[textureCount];
    System.arraycopy(rawText, 0, textures, 0, textureCount);

    final String[] bones = new String[boneCount];
    System.arraycopy(rawText, textureCount, bones, 0, boneCount);

    return new AspectBmsh(version, textures, bones, vertexCount, submeshCount, renderFlags);
  }

  private static List<AspectBonh> readBonh(final InputStream file, final AspectBmsh bmsh) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final List<AspectBonh> bones = new ArrayList<>();
    for(int boneIndex = 0; boneIndex < bmsh.bones().length; boneIndex++) {
      final int index = readInt(file);
      final int parent = readInt(file);
      final int flags = readInt(file);

      bones.add(new AspectBonh(version, index, parent, flags));
    }

    return bones;
  }

  private static AspectBsub readBsub(final InputStream file) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final int submeshIndex;
    if(version.normalized > 40) {
      submeshIndex = readInt(file);
    } else {
      submeshIndex = readInt(file) + 1;
    }

    final int textureCount = readInt(file);
    final int vertexCount = readInt(file);
    final int cornerCount = readInt(file);
    final int faceCount = readInt(file);

    return new AspectBsub(version, submeshIndex, textureCount, vertexCount, cornerCount, faceCount);
  }

  private static List<AspectBsmm> readBsmm(final InputStream file) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final List<AspectBsmm> bsmm = new ArrayList<>();
    final int textureCount = readInt(file);
    for(int i = 0; i < textureCount; i++) {
      final int textureIndex = readInt(file);
      final int faceSpan = readInt(file);
      bsmm.add(new AspectBsmm(version, textureIndex, faceSpan));
    }

    return bsmm;
  }

  private static List<AspectBvtx> readBvtx(final InputStream file) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final List<AspectBvtx> bvtx = new ArrayList<>();
    final int vertexCount = readInt(file);
    for(int i = 0; i < vertexCount; i++) {
      final Vector3f pos = readVec3(file);
      bvtx.add(new AspectBvtx(version, pos));
    }

    return bvtx;
  }

  private static List<AspectBcrn> readBcrn(final InputStream file) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final List<AspectBcrn> bcrn = new ArrayList<>();
    final int cornerCount = readInt(file);
    for(int i = 0; i < cornerCount; i++) {
      final int vertexIndex = readInt(file);
      final Vector3f normal = readVec3(file);
      final int colour = readInt(file);
      readInt(file); // unused
      final Vector2f uv = readVec2(file);
      bcrn.add(new AspectBcrn(version, vertexIndex, normal, colour, uv));
    }

    return bcrn;
  }

  private static List<AspectWcrn> readWcrn(final InputStream file) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final List<AspectWcrn> wcrn = new ArrayList<>();
    final int cornerCount = readInt(file);
    for(int i = 0; i < cornerCount; i++) {
      final Vector3f pos = readVec3(file);
      final Vector4f weight = readVec4(file);
      final Vector4i bone = readVec4b(file);

      if(version.normalized > 40) {
        bone.x++;
        bone.y++;
        bone.z++;
        bone.w++;
      }

      final Vector3f normal = readVec3(file);
      final Vector4i colour = readVec4b(file);
      final Vector2f uv = readVec2(file);

      //TODO remove null bone/weights? See import script line#347

      wcrn.add(new AspectWcrn(version, pos, weight, bone, normal, colour, uv));
    }

    return wcrn;
  }

  private static List<AspectBvmp> readBvmp(final InputStream file, final AspectBsub bsub) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final List<AspectBvmp> bvmp = new ArrayList<>();
    for(int vertexIndex = 0; vertexIndex < bsub.vertexCount(); vertexIndex++) {
      final int count = readInt(file);
      final int[] indices = new int[count];

      for(int cornerIndex = 0; cornerIndex < count; cornerIndex++) {
        indices[cornerIndex] = readInt(file) + 1;
      }

      bvmp.add(new AspectBvmp(version, indices));
    }

    return bvmp;
  }

  private static AspectBtri readBtri(final InputStream file, final AspectBsub bsub) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final int faceCount = readInt(file);

    final int[] cornerStarts = new int[bsub.subTextures()];
    final int[] cornerSpans = new int[bsub.subTextures()];

    if(version.normalized < 22) {
      for(int textureIndex = 0; textureIndex < bsub.subTextures(); textureIndex++) {
        cornerStarts[textureIndex] = 0;
        cornerSpans[textureIndex] = readInt(file);
      }
    } else if(version.normalized == 22) {
      for(int textureIndex = 0; textureIndex < bsub.subTextures(); textureIndex++) {
        cornerSpans[textureIndex] = readInt(file);
      }

      for(int textureIndex = 0; textureIndex < bsub.subTextures() - 1; textureIndex++) {
        cornerStarts[textureIndex + 1] = cornerStarts[textureIndex] + cornerSpans[textureIndex];
      }
    } else {
      for(int textureIndex = 0; textureIndex < bsub.subTextures(); textureIndex++) {
        cornerStarts[textureIndex] = readInt(file);
        cornerSpans[textureIndex] = readInt(file);
      }
    }

    final Vector3i[] cornerIndices = new Vector3i[faceCount];
    for(int faceIndex = 0; faceIndex < faceCount; faceIndex++) {
      cornerIndices[faceIndex] = readVec3i(file);
    }

    return new AspectBtri(version, cornerStarts, cornerSpans, cornerIndices);
  }

  private static List<AspectBvwl> readBvwl(final InputStream file, final AspectBmsh bmsh) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final List<AspectBvwl> bvwl = new ArrayList<>();
    for(int boneIndex = 0; boneIndex < bmsh.bones().length; boneIndex++) {
      final int count = readInt(file);

      final int[] cornerIds = new int[count];
      final int[] cornerWeights = new int[count];

      for(int cornerIndex = 0; cornerIndex < count; cornerIndex++) {
        cornerIds[cornerIndex] = readInt(file);
        cornerWeights[cornerIndex] = readInt(file);
      }

      bvwl.add(new AspectBvwl(version, cornerIds, cornerWeights));
    }

    return bvwl;
  }

  private static List<AspectStch> readStch(final InputStream file) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final List<AspectStch> stch = new ArrayList<>();
    final int stitchCount = readInt(file);
    for(int stitchIndex = 0; stitchIndex < stitchCount; stitchIndex++) {
      final int token = readInt(file);
      final int vertexCount = readInt(file);

      final int[] vertexIndices = new int[vertexCount];
      for(int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
        vertexIndices[vertexIndex] = readInt(file);
      }

      stch.add(new AspectStch(version, token, vertexIndices));
    }

    return stch;
  }

  private static List<AspectRpos> readRpos(final InputStream file) throws IOException {
    final AspectVersion version = AspectVersion.fromNum(readInt(file));

    final List<AspectRpos> rpos = new ArrayList<>();
    final int boneCount = readInt(file);
    for(int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
      final Quaternionf[] rotations = new Quaternionf[2];
      final Vector3f[] positions = new Vector3f[2];

      for(int i = 0; i < 2; i++) {
        rotations[i] = readQuat(file);
        positions[i] = readVec3(file);
      }

      rpos.add(new AspectRpos(version, rotations, positions));
    }

    return rpos;
  }

  private static void readBbox(final InputStream file) throws IOException {
    AspectVersion.fromNum(readInt(file));

    if(readInt(file) != 0) {
      throw new IOException("Invalid BBOX definition");
    }
  }

  private static void readBend(final InputStream file) throws IOException {
    if(!"INFO".equals(read4cc(file))) {
      throw new IOException("Expected INFO header for BEND section");
    }

    final int entryCount = readInt(file);
    for(int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
      final StringBuilder builder = new StringBuilder();

      while(true) {
        final char b = (char)file.read();

        if(b == '\0') {
          break;
        }

        builder.append(b);
      }

      System.out.println(builder);
    }
  }

  private static String read4cc(final InputStream file) throws IOException {
    final byte[] raw = new byte[4];

    if(file.read(raw) < 4) {
      return "";
    }

    return new String(raw);
  }

  private static String readString(final InputStream file, final int length) throws IOException {
    final byte[] raw = new byte[length];

    if(file.read(raw) < raw.length) {
      throw new EOFException("End of file reached");
    }

    return new String(raw);
  }

  private static int readInt(final InputStream file) throws IOException {
    final byte[] raw = new byte[4];

    if(file.read(raw) < raw.length) {
      throw new EOFException("End of file reached");
    }

    return (raw[3] & 0xff) << 24 | (raw[2] & 0xff) << 16 | (raw[1] & 0xff) << 8 | raw[0] & 0xff;
  }

  private static float readFloat(final InputStream file) throws IOException {
    return Float.intBitsToFloat(readInt(file));
  }

  private static Vector2f readVec2(final InputStream file) throws IOException {
    return new Vector2f(readFloat(file), readFloat(file));
  }

  private static Vector3f readVec3(final InputStream file) throws IOException {
    return new Vector3f(readFloat(file), readFloat(file), readFloat(file));
  }

  private static Vector3i readVec3i(final InputStream file) throws IOException {
    return new Vector3i(readInt(file), readInt(file), readInt(file));
  }

  private static Vector4f readVec4(final InputStream file) throws IOException {
    return new Vector4f(readFloat(file), readFloat(file), readFloat(file), readFloat(file));
  }

  private static Vector4i readVec4b(final InputStream file) throws IOException {
    return new Vector4i(file.read(), file.read(), file.read(), file.read());
  }

  private static Quaternionf readQuat(final InputStream file) throws IOException {
    return new Quaternionf(readFloat(file), readFloat(file), readFloat(file), readFloat(file));
  }
}
