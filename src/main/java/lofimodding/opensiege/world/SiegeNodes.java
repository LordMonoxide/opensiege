package lofimodding.opensiege.world;

import lofimodding.opensiege.go.GameObject;
import lofimodding.opensiege.go.GoType;
import lofimodding.opensiege.go.GoValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@GoType(type = "siege_nodes")
public class SiegeNodes extends GameObject implements Iterable<SiegeNodes.MeshFile> {
  @GoValue(name = "mesh_file")
  private final List<MeshFile> meshFile = new ArrayList<>();

  @Override
  public Iterator<MeshFile> iterator() {
    return this.meshFile.iterator();
  }

  public List<MeshFile> getMeshes() {
    return this.meshFile;
  }

  public static class MeshFile {
    @GoValue
    private String filename;

    @GoValue
    private int guid;

    public String getFilename() {
      return this.filename;
    }

    public int getGuid() {
      return this.guid;
    }
  }
}
