package lofimodding.opensiege.formats.tank;

public record TankHeader(String productId, String tankId, int headerVersion, int dirSetOffset, int fileSetOffset, int indexSize, int dataOffset) {

}
