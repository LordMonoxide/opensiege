package lofimodding.opensiege;

import lofimodding.opensiege.formats.gas.GasEntry;
import lofimodding.opensiege.formats.gas.GasLoader;
import lofimodding.opensiege.go.GoDb;
import lofimodding.opensiege.world.StartGroup;
import lofimodding.opensiege.world.World;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
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
    final Future<Path> tankFuture = exec.submit(() -> Paths.get(new URI("tank:" + URLEncoder.encode(path.toAbsolutePath().normalize().toString(), StandardCharsets.UTF_8) + "!/")));
    final Future<GoDb> goDbFuture = exec.submit(GoDb::new);

    final Path p = tankFuture.get();
    final GoDb go = goDbFuture.get();

    exec.shutdown();

    final Map<String, String> maps = new HashMap<>();
    final Map<String, GasEntry> mapGas = new HashMap<>();

    System.out.println("Searching for maps...");

    try(final DirectoryStream<Path> ds = Files.newDirectoryStream(p.resolve("world/maps"), Files::isDirectory)) {
      for(final Path child : ds) {
        final GasEntry root = GasLoader.load(Files.newInputStream(child.resolve("main.gas")));
        final GasEntry data = root.getChild("t:map,n:map");
        maps.put(child.getFileName().toString(), data.getString("screen_name") + " - " + data.getString("description"));
        mapGas.put(child.getFileName().toString(), root);
      }
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

    final World map = go.get(World.class, "map");
    final Path mapPath = p.resolve("world").resolve("maps").resolve(map.getName());

    final GasEntry startPositions = GasLoader.load(Files.newInputStream(mapPath.resolve("info").resolve("start_positions.gas")));
    go.addObject(startPositions.getChild("start_positions"));

    final Map<String, String> regions = new HashMap<>();

    for(final Map.Entry<String, StartGroup> entry : go.get(StartGroup.class).entrySet()) {
      final StartGroup startGroup = entry.getValue();
      regions.put(entry.getKey(), startGroup.getScreenName() + " - " + startGroup.getDescription());
    }

    String regionName;

    while(true) {
      System.out.println();
      System.out.println("Available regions:");

      for(final Map.Entry<String, String> entry : regions.entrySet()) {
        System.out.println(entry.getKey() + ": " + entry.getValue());
      }

      System.out.println();
      System.out.print("Which region would you like to load? ");

      regionName = scanner.nextLine();

      if(regions.containsKey(regionName)) {
        break;
      }

      System.out.println();
      System.out.println("Please enter a valid region ID.");
    }

    final StartGroup.StartPosition startPosition = go.get(StartGroup.class, regionName).getStartPositions().get(0);

    String regionId = null;

    // Find the starting region based on camera location
    try(final DirectoryStream<Path> nodeStream = Files.newDirectoryStream(mapPath.resolve("regions"), Files::isDirectory)) {
      for(final Path region : nodeStream) {
        final GasEntry nodes = GasLoader.load(Files.newInputStream(region.resolve("index").resolve("streamer_node_index.gas")));

        final List<Integer> nodeGuids = (List<Integer>)nodes.getChild("streamer_node_index").get("");
        if(nodeGuids.contains(startPosition.getPosition().getNodeId())) {
          regionId = region.getFileName().toString();
        }
      }
    }

    if(regionId == null) {
      throw new RuntimeException("Couldn't find region ID for map " + map.getName());
    }

    new Game(p, go, mapId, regionId, startPosition.getPosition().getNodeId());
  }
}
