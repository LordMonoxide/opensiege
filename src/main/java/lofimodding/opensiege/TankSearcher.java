package lofimodding.opensiege;

import lofimodding.opensiege.formats.tank.TankManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class TankSearcher {
  private TankSearcher() { }

  public static void main(final String[] args) throws IOException {
    final TankManager tankManager = new TankManager(Paths.get("C:", "Program Files (x86)", "Steam", "steamapps", "common", "Dungeon Siege 1"));

    final List<String> refs = new ArrayList<>();

    final String search = String.join(" ", args).toLowerCase();

    tankManager.getFiles().parallelStream().forEach(filename -> {
      if(filename.endsWith(".lqd20")) {
        return;
      }

      try {
        final InputStream stream = tankManager.getFileByPath(filename);
        final String data = new String(stream.readAllBytes());

        if(data.toLowerCase().contains(search)) {
          refs.add(filename);
        }
      } catch(final IOException e) {
        throw new RuntimeException(e);
      }
    });

    refs.forEach(System.out::println);
  }
}
