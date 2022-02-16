package lofimodding.opensiege.world;

import lofimodding.opensiege.formats.gas.GasEntry;
import lofimodding.opensiege.go.GoLoader;

public class RegionLoader implements GoLoader<Region> {
  @Override
  public Region load(final String name, final GasEntry root) {
    return new Region();
  }
}
