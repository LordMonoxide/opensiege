package lofimodding.opensiege.formats.tank;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.InflaterInputStream;

public record Tank(Path path, TankHeader header, Int2ObjectMap<TankDirectoryEntry> directoryEntries, Int2ObjectMap<TankFileEntry> fileEntries, Map<String, TankFileEntry> paths) {
  public InputStream getFile(final String filename) throws IOException {
    final TankFileEntry fileEntry = this.paths.get(filename);

    if(fileEntry == null) {
      throw new FileNotFoundException(filename + " not found in tank archive");
    }

    final FileInputStream file = new FileInputStream(this.path.toFile());

    if(!fileEntry.format().isCompressed()) {
      file.skip(this.header.dataOffset() + fileEntry.dataOffset());
      final byte[] data = new byte[fileEntry.entrySize()];
      file.read(data);
      return new ByteArrayInputStream(data);
    }

    final byte[][] allData = new byte[fileEntry.compressionHeader().chunkHeaders().length][];
    int totalSize = 0;

    final TankChunkHeader[] chunkHeaders = fileEntry.compressionHeader().chunkHeaders();
    for(int chunkIndex = 0; chunkIndex < chunkHeaders.length; chunkIndex++) {
      final TankChunkHeader chunkHeader = chunkHeaders[chunkIndex];

      file.getChannel().position(this.header.dataOffset() + fileEntry.dataOffset() + chunkHeader.offset());

      if(chunkHeader.isCompressed()) {
        final InflaterInputStream zlibInput = new InflaterInputStream(file);

        final byte[] uncompressedData = new byte[chunkHeader.uncompressedBytes() + chunkHeader.extraBytes()];
        int readBytes = 0;
        while(zlibInput.available() != 0) {
          readBytes += zlibInput.read(uncompressedData, readBytes, uncompressedData.length - readBytes);
        }

        if(readBytes < uncompressedData.length) {
          throw new EOFException("Unexpected end of file");
        }

        allData[chunkIndex] = uncompressedData;
      } else {
        final byte[] uncompressedData = new byte[chunkHeader.uncompressedBytes()];
        if(file.read(uncompressedData) < uncompressedData.length) {
          throw new EOFException("Unexpected end of file");
        }

        allData[chunkIndex] = uncompressedData;
      }

      totalSize += allData[chunkIndex].length;
    }

    final byte[] data = new byte[totalSize];
    int dataIndex = 0;

    for(final byte[] chunk : allData) {
      System.arraycopy(chunk, 0, data, dataIndex, chunk.length);
      dataIndex += chunk.length;
    }

    return new ByteArrayInputStream(data);
  }

  @Override
  public String toString() {
    return "Tank " + this.path;
  }
}
