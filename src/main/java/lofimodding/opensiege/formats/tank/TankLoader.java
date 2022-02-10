package lofimodding.opensiege.formats.tank;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class TankLoader {
  private TankLoader() { }

  public static Tank load(final Path path) throws IOException {
    if(!Files.isRegularFile(path)) {
      throw new IOException(path + " is not a file");
    }

    final long fileSize = Files.size(path);

    System.out.println("Loading tank file " + path + " (" + fileSize + " bytes)");

    final long startTime = System.nanoTime();

    final FileInputStream file = new FileInputStream(path.toFile());

    // Header
    System.out.println("Loading header...");

    final String productId = readString(file, 4);
    final String tankId = readString(file, 4);
    final int headerVersion = readInt(file);
    final int dirSetOffset = readInt(file);
    final int fileSetOffset = readInt(file);
    final int indexSize = readInt(file);
    final int dataSetOffset = readInt(file);

    final TankHeader header = new TankHeader(productId, tankId, headerVersion, dirSetOffset, fileSetOffset, indexSize, dataSetOffset);

    // Directory entries
    System.out.println("Finding directory entries...");

    file.getChannel().position(dirSetOffset);

    final int dirCount = readInt(file);
    final int[] dirOffsets = new int[dirCount];
    for(int dirIndex = 0; dirIndex < dirCount; dirIndex++) {
      final int dirOffset = readInt(file);

      if(dirSetOffset + dirOffset >= fileSize) {
        System.err.println("Directory " + dirIndex + " offset is corrupt");
        continue;
      }

      dirOffsets[dirIndex] = dirOffset;
    }

    System.out.println("Loading " + dirCount + " directory entries...");

    final Int2ObjectMap<TankDirectoryEntry> dirEntries = new Int2ObjectOpenHashMap<>();
    for(int dirIndex = 0; dirIndex < dirCount; dirIndex++) {
      final int dirOffset = dirOffsets[dirIndex];

      file.getChannel().position(dirSetOffset + dirOffset);

      final int parentOffset = readInt(file);
      final int childCount = readInt(file);
      final long fileTime = readLong(file);
      final String dirName = readNString(file);
      final int[] childOffsets = new int[childCount];

      for(int childIndex = 0; childIndex < childCount; childIndex++) {
        final int childOffset = dirSetOffset + readInt(file);
        childOffsets[childIndex] = childOffset;
      }

      dirEntries.put(dirOffset, new TankDirectoryEntry(parentOffset, childCount, fileTime, dirName, childOffsets));
    }

    // File entries
    System.out.println("Finding file entries...");

    file.getChannel().position(fileSetOffset);

    final int fileCount = readInt(file);
    final int[] fileOffsets = new int[fileCount];
    for(int fileIndex = 0; fileIndex < fileCount; fileIndex++) {
      final int fileOffset = readInt(file);

      if(dirSetOffset + fileOffset >= fileSize) {
        System.err.println("File " + fileIndex + " offset is corrupt");
        continue;
      }

      fileOffsets[fileIndex] = fileOffset;
    }

    System.out.println("Loading " + fileCount + " file entries...");

    final Int2ObjectMap<TankFileEntry> fileEntries = new Int2ObjectOpenHashMap<>();
    for(int fileIndex = 0; fileIndex < fileCount; fileIndex++) {
      final int fileOffset = fileOffsets[fileIndex];

      file.getChannel().position(fileSetOffset + fileOffset);

      final byte[] data = file.readNBytes(28);

      final int parentOffset = readInt(data, 0);
      final int entrySize = readInt(data, 4);
      final int dataOffset = readInt(data, 8);
      final int crc32 = readInt(data, 12);
      final long fileTime = readLong(data, 16);
      final TankFormat format = TankFormat.fromIndex(readInt16(data, 24));
      final TankFlags flags = TankFlags.fromCode(readInt16(data, 26));
      final String name = readNString(file);

      @Nullable final TankFileCompressionHeader compressionHeader;
      if(format.isCompressed() && entrySize != 0) {
        final int compressedSize = readInt(file);

        if(compressedSize > fileSize) {
          System.err.println("File " + name + " is corrupt - compressed size exceeds total tank size");
          continue;
        }

        final int chunkSize = readInt(file);
        final int chunkCount = chunkSize != 0 ? (int)Math.ceil((double)entrySize / chunkSize) : 0;
        final TankChunkHeader[] chunkHeaders = new TankChunkHeader[chunkCount];

        // Read as much data up front as possible for efficiency
        final int chunkStride = 4 * 4;
        final byte[] chunkData = new byte[chunkCount * chunkStride];
        if(file.read(chunkData) < chunkData.length) {
          throw new EOFException("Unexpected end of file");
        }

        for(int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
          final int uncompressedBytes = readInt(chunkData, chunkIndex * chunkStride);
          final int compressedBytes = readInt(chunkData, chunkIndex * chunkStride + 4);
          final int extraBytes = readInt(chunkData, chunkIndex * chunkStride + 8);
          final int offset = readInt(chunkData, chunkIndex * chunkStride + 12);

          if(uncompressedBytes < compressedBytes) {
            throw new EOFException("Malformed file " + name + " - compressed size larger than uncompressed size");
          }

          chunkHeaders[chunkIndex] = new TankChunkHeader(uncompressedBytes, compressedBytes, extraBytes, offset);
        }

        compressionHeader = new TankFileCompressionHeader(compressedSize, chunkSize, chunkHeaders);
      } else {
        compressionHeader = null;
      }

      fileEntries.put(fileOffset, new TankFileEntry(parentOffset, entrySize, dataOffset, crc32, fileTime, format, flags, name, compressionHeader));
    }

    System.out.println("Building path tree...");

    final Map<String, TankFileEntry> paths = buildPaths(dirEntries, fileEntries);

    final long endTime = System.nanoTime();

    System.out.println("Loaded " + path + " in " + (endTime - startTime) / 1000000 + "ms");

    return new Tank(path, header, dirEntries, fileEntries, paths);
  }

  private static Map<String, TankFileEntry> buildPaths(final Int2ObjectMap<TankDirectoryEntry> dirEntries, final Int2ObjectMap<TankFileEntry> fileEntries) {
    final Map<String, TankFileEntry> paths = new HashMap<>();

    for(final TankFileEntry fileEntry : fileEntries.values()) {
      final StringBuilder path = new StringBuilder();

      if(fileEntry.parentOffset() != 0) {
        buildPath(path, dirEntries, dirEntries.get(fileEntry.parentOffset()));
      }

      path.append('/').append(fileEntry.name());
      paths.put(path.toString(), fileEntry);
    }

    return paths;
  }

  private static void buildPath(final StringBuilder path, final Int2ObjectMap<TankDirectoryEntry> dirEntries, final TankDirectoryEntry dirEntry) {
    if(dirEntry.parentOffset() == 0) {
      return;
    }

    buildPath(path, dirEntries, dirEntries.get(dirEntry.parentOffset()));
    path.append('/').append(dirEntry.dirName());
  }

  private static String readString(final InputStream file, final int length) throws IOException {
    final byte[] raw = new byte[length];
    file.read(raw);
    return new String(raw);
  }

  private static String readNString(final InputStream file) throws IOException {
    final int length = readInt16(file);
    final String string = readString(file, length);

    // 4-byte align
    file.skip(4 - (length + 2) % 4);

    return string;
  }

  private static int readInt16(final InputStream file) throws IOException {
    final byte[] raw = new byte[2];
    file.read(raw);
    return (raw[1] & 0xff) << 8 | raw[0] & 0xff;
  }

  private static int readInt16(final byte[] data, final int offset) throws IOException {
    return (data[offset + 1] & 0xff) << 8 | data[offset] & 0xff;
  }

  private static final byte[] readIntBuf = new byte[4];

  private static int readInt(final InputStream file) throws IOException {
    file.read(readIntBuf);
    return readInt(readIntBuf, 0);
  }

  private static int readInt(final byte[] data, final int offset) {
    return (data[offset + 3] & 0xff) << 24 | (data[offset + 2] & 0xff) << 16 | (data[offset + 1] & 0xff) << 8 | data[offset] & 0xff;
  }

  private static long readLong(final InputStream file) throws IOException {
    final byte[] raw = new byte[8];
    file.read(raw);
    return (raw[7] & 0xffL) << 56 | (raw[6] & 0xffL) << 48 | (raw[5] & 0xffL) << 40 | (raw[4] & 0xffL) << 32 | (raw[3] & 0xffL) << 24 | (raw[2] & 0xffL) << 16 | (raw[1] & 0xffL) << 8 | raw[0] & 0xffL;
  }

  private static long readLong(final byte[] data, final int offset) {
    return (data[offset + 7] & 0xffL) << 56 | (data[offset + 6] & 0xffL) << 48 | (data[offset + 5] & 0xffL) << 40 | (data[offset + 4] & 0xffL) << 32 | (data[offset + 3] & 0xffL) << 24 | (data[offset + 2] & 0xffL) << 16 | (data[offset + 1] & 0xffL) << 8 | data[offset] & 0xffL;
  }
}
