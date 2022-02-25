package lofimodding.opensiege.formats.siegenode;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.List;

public record Sno(SnoHeader header, List<SnoSpot> spots, Int2ObjectMap<SnoDoor> doors, List<SnoCorner> corners, List<SnoSurface> surfaces) {
  public SnoDoor getDoor(final int id) {
    for(final SnoDoor door : this.doors().values()) {
      if(door.index() == id) {
        return door;
      }
    }

    throw new RuntimeException("No such door " + id);
  }
}
