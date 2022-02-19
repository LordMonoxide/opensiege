package lofimodding.opensiege;

import lofimodding.opensiege.formats.gas.GasEntry;
import lofimodding.opensiege.formats.gas.GasLoader;
import lofimodding.opensiege.formats.tank.TankFileSystem;
import lofimodding.opensiege.formats.tank.TankFileSystemProvider;
import lofimodding.opensiege.formats.tank.TankManager;
import lofimodding.opensiege.go.GoDb;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Launcher {
  private Launcher() { }

  public static void main(final String[] args) throws IOException, ExecutionException, InterruptedException {
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

    final ExecutorService exec = Executors.newCachedThreadPool();
    final Future<TankManager> tankManagerFuture = exec.submit(() -> new TankManager(path));
    final Future<GoDb> goDbFuture = exec.submit(GoDb::new);

    final TankManager tankManager = tankManagerFuture.get();
    final GoDb go = goDbFuture.get();

    exec.shutdown();

    final TankFileSystem fs = new TankFileSystem(new TankFileSystemProvider(), tankManager, null);
    final Path p = fs.getPath("/");

    Files.newDirectoryStream(p).forEach(f -> {
      System.out.println(f);
    });

    Path r = Paths.get(URI.create("tank:" + path.toString().replace('\\', '/') + "!/"));;

    final Map<String, String> maps = new HashMap<>();
    final Map<String, GasEntry> mapGas = new HashMap<>();

    System.out.println("Searching for maps...");

    for(final String child : tankManager.getSubdirectories("/world/maps")) {
      final GasEntry root = GasLoader.load(tankManager.getFileByPath("/world/maps/" + child + "/main.gas"));
      final GasEntry data = root.getChild("t:map,n:map");
      maps.put(child, data.getString("screen_name") + " - " + data.getString("description"));
      mapGas.put(child, root);
    }

    System.out.println();
    System.out.println("---------------------------------------------");

    final Scanner scanner = new Scanner(System.in);

    String mapId;

    while(true) {
      System.out.println();
      System.out.println("Available maps:");

      for(final Map.Entry<String, String> entry : maps.entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
      }

      System.out.println();
      System.out.print("Which map would you like to load? ");

      mapId = scanner.nextLine();

      if(maps.containsKey(mapId)) {
        break;
      }

      System.out.println();
      System.out.println("Please enter a valid map ID.");
    }

    go.addObject(mapGas.get(mapId));

    new Game(tankManager, go, mapId);
  }
}
