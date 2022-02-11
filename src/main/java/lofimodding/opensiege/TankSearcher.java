package lofimodding.opensiege;

import lofimodding.opensiege.formats.tank.Tank;
import lofimodding.opensiege.formats.tank.TankLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class TankSearcher {
  private TankSearcher() { }

  public static void main(final String[] args) throws IOException {
    final Tank tank = TankLoader.load(Paths.get("C:", "Program Files (x86)", "Steam", "steamapps", "common", "Dungeon Siege 1", "Resources", "Logic.dsres"));

    final List<String> refs = new ArrayList<>();

    for(final String filename : tank.paths().keySet()) {
      final InputStream stream = tank.getFileByPath(filename);
      final String data = new String(stream.readAllBytes());

      if(data.toLowerCase().contains("components")) {
        refs.add(filename);
      }
    }

    refs.forEach(System.out::println);
  }
}
