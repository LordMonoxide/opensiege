package lofimodding.opensiege.formats.tank;

public record TankFileCompressionHeader(int compressedSize, int chunkSize, TankChunkHeader[] chunkHeaders) {

}
