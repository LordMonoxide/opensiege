package lofimodding.opensiege.formats.tank;

public record TankDirectoryEntry(int parentOffset, int childCount, long fileTime, String dirName, int[] childOffsets) {

}
