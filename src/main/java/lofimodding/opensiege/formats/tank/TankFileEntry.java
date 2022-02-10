package lofimodding.opensiege.formats.tank;

import javax.annotation.Nullable;

public record TankFileEntry(int parentOffset, int entrySize, int dataOffset, int crc32, long fileTime, TankFormat format, TankFlags flags, String name, @Nullable TankFileCompressionHeader compressionHeader) {

}
