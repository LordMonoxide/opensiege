package lofimodding.opensiege.formats.tank;

public record TankChunkHeader(int uncompressedBytes, int compressedBytes, int extraBytes, int offset) {
  public boolean isCompressed() {
    return this.compressedBytes != this.uncompressedBytes;
  }
}
