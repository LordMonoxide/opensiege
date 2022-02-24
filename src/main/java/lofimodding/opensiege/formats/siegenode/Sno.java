package lofimodding.opensiege.formats.siegenode;

import java.util.List;

public record Sno(SnoHeader header, List<SnoSpot> spots, List<SnoDoor> doors, List<SnoCorner> corners, List<SnoSurface> surfaces) {

}
