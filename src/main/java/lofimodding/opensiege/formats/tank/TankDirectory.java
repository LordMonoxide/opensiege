package lofimodding.opensiege.formats.tank;

import java.util.HashMap;
import java.util.Map;

public class TankDirectory {
  public final Map<String, TankDirectory> directories = new HashMap<>();
  public final Map<String, Tank> files = new HashMap<>();
}
