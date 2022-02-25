package lofimodding.opensiege.formats.tank;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TankManager {
  private final List<Tank> tanks = new ArrayList<>();
  private final Map<String, Tank> paths = new HashMap<>();
  private final Map<String, String> files = new HashMap<>();
  private final Set<String> dirs = new HashSet<>();
  private final TankDirectory root = new TankDirectory();

  public TankManager(final Path installPath) throws IOException {
    System.out.println("Loading resources...");

    final List<Path> files = new ArrayList<>();

    try(final DirectoryStream<Path> ds = Files.newDirectoryStream(installPath.resolve("Resources"), child -> Files.isRegularFile(child) && child.toString().endsWith(".dsres"))) {
      ds.forEach(files::add);
    }

    try(final DirectoryStream<Path> ds = Files.newDirectoryStream(installPath.resolve("Maps"), child -> Files.isRegularFile(child) && child.toString().endsWith(".dsmap"))) {
      ds.forEach(files::add);
    }

    files.parallelStream().map(path1 -> {
      try {
        return TankLoader.load(path1);
      } catch(final IOException e) {
        throw new RuntimeException(e);
      }
    }).forEach(this.tanks::add);

    System.out.println("Sorting resources...");

    this.tanks.sort(Comparator.comparingInt(tank -> tank.header().priority().code));

    System.out.println("Final resource order:");

    for(final Tank tank : this.tanks) {
      System.out.println(tank.header().priority() + ": " + tank.path());
    }

    System.out.println("Building file map...");

    //TODO is this okay? .gas files especially - do we need to actually load these in order of priority and overwrite config values?

    for(final Tank tank : this.tanks) {
      for(final String path : tank.filePaths().keySet()) {
        final String[] pathParts = stripLeadingSlash(path).split("/");

        TankDirectory dir = this.root;
        for(int i = 0; i < pathParts.length - 1; i++) {
          dir = dir.directories.computeIfAbsent(pathParts[i], key -> new TankDirectory());
        }

        dir.files.put(pathParts[pathParts.length - 1].toLowerCase(), tank);
        this.files.put(pathParts[pathParts.length - 1].toLowerCase(), path);

        this.paths.put(path.toLowerCase(), tank);
      }

      this.dirs.addAll(tank.directoryPaths().keySet());
    }

    System.out.println("All resources loaded.");
  }

  public Set<String> getFiles() {
    return this.paths.keySet();
  }

  public byte[] getFileByPath(final String filename) throws IOException {
    final Tank tank = this.paths.get(filename.toLowerCase());

    if(tank == null) {
      throw new FileNotFoundException(filename + " not found");
    }

    return tank.getFileByPath(filename);
  }

  public TankFileEntry getFileInfo(final String filename) throws IOException {
    final Tank tank = this.paths.get(filename.toLowerCase());

    if(tank == null) {
      throw new FileNotFoundException(filename + " not found");
    }

    return tank.filePaths().get(filename);
  }

  public String lookupPath(final String file) {
    return this.files.get(file.toLowerCase());
  }

  public boolean isFile(final String path) {
    return this.files.containsKey(path.toLowerCase());
  }

  public boolean isDir(final String path) {
    return this.dirs.contains(path.toLowerCase());
  }

  public Set<String> getChildren(final String path) {
    TankDirectory dir = this.root;

    final String strippedPath = stripLeadingSlash(path);
    if(!strippedPath.isEmpty()) {
      final String[] pathParts = strippedPath.split("/");
      for(final String part : pathParts) {
        dir = dir.directories.get(part);
      }
    }

    final Set<String> children = new HashSet<>();
    children.addAll(dir.directories.keySet());
    children.addAll(dir.files.keySet());

    return children;
  }

  private static String stripLeadingSlash(final String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }
}
