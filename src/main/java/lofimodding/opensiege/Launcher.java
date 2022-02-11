package lofimodding.opensiege;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Launcher {
  private Launcher() { }

  public static void main(final String[] args) throws IOException {
    System.out.println("OpenSiege start!");

    final Path path;
    if(args.length != 0) {
      path = Paths.get(String.join(File.pathSeparator, args));
    } else {
      path = Paths.get(".");
    }

    if(!Files.isDirectory(path)) {
      throw new RuntimeException("Failed to start - install path does not exist (" + path + ')');
    }

    if(!Files.isReadable(path)) {
      throw new RuntimeException("Failed to start - install path is not readable (" + path + ')');
    }

    new Game(path);
  }
}
