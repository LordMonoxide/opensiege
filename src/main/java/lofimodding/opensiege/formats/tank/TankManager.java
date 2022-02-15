package lofimodding.opensiege.formats.tank;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TankManager {
  private final List<Tank> tanks = new ArrayList<>();
  private final Map<String, Tank> paths = new HashMap<>();
  private final Map<String, String> files = new HashMap<>();
  private final TankDirectory root = new TankDirectory();

  public TankManager(final Path installPath) throws IOException {
    System.out.println("Loading resources...");

    for(final Path child : Files.newDirectoryStream(installPath.resolve("Resources"), child -> Files.isRegularFile(child) && child.toString().endsWith(".dsres"))) {
      this.tanks.add(TankLoader.load(child));
    }

    for(final Path child : Files.newDirectoryStream(installPath.resolve("Maps"), child -> Files.isRegularFile(child) && child.toString().endsWith(".dsmap"))) {
      this.tanks.add(TankLoader.load(child));
    }

    System.out.println("Sorting resources...");

    this.tanks.sort(Comparator.comparingInt(tank -> tank.header().priority().code));

    System.out.println("Final resource order:");

    for(final Tank tank : this.tanks) {
      System.out.println(tank.header().priority() + ": " + tank.path());
    }

    System.out.println("Building file map...");

    //TODO is this okay? .gas files especially - do we need to actually load these in order of priority and overwrite config values?

    for(final Tank tank : this.tanks) {
      for(final String path : tank.paths().keySet()) {
        final String[] pathParts = stripLeadingSlash(path).split("/");

        TankDirectory dir = this.root;
        for(int i = 0; i < pathParts.length - 1; i++) {
          dir = dir.directories.computeIfAbsent(pathParts[i], key -> new TankDirectory());
        }

        dir.files.put(pathParts[pathParts.length - 1], tank);
        this.files.put(pathParts[pathParts.length - 1], path);

        this.paths.put(path, tank);
      }
    }

    System.out.println("All resources loaded.");
  }

  public Set<String> getFiles() {
    return this.paths.keySet();
  }

  public InputStream getFileByPath(final String filename) throws IOException {
    final Tank tank = this.paths.get(filename);

    if(tank == null) {
      throw new FileNotFoundException(filename + " not found");
    }

    return tank.getFileByPath(filename);
  }

  public String lookupPath(final String file) {
    return this.files.get(file);
  }

  public Iterable<String> getSubdirectories(final String path) {
    final String[] pathParts = stripLeadingSlash(path).split("/");
    TankDirectory dir = this.root;
    for(final String part : pathParts) {
      dir = dir.directories.get(part);
    }

    return dir.directories.keySet();
  }

  private static String stripLeadingSlash(final String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }
}
