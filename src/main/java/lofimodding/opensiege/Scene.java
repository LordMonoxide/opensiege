package lofimodding.opensiege;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lofimodding.opensiege.formats.siegenode.Sno;
import lofimodding.opensiege.formats.siegenode.SnoLoader;
import lofimodding.opensiege.formats.siegenode.SnoRenderer;
import lofimodding.opensiege.gfx.Texture;
import lofimodding.opensiege.go.GoDb;
import lofimodding.opensiege.world.SiegeNodes;
import lofimodding.opensiege.world.Snode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Scene {
  private final Path root;
  private final TextureManager textureManager;
  private final GoDb goDb;

  private final Int2ObjectMap<SceneChunk> cache = new Int2ObjectOpenHashMap<>();

  private SceneChunk chunk;

  public Scene(final Path root, final TextureManager textureManager, final GoDb goDb) {
    this.root = root;
    this.textureManager = textureManager;
    this.goDb = goDb;
  }

  public void setRegion(final int guid) throws IOException {
    if(this.cache.containsKey(guid)) {
      this.chunk = this.cache.get(guid);
    }

    final Snode snode = this.goDb.get(Snode.class, "0x" + Integer.toHexString(guid));
    final String meshFile = this.findMeshFilename(snode.getMeshGuid());

    if(meshFile == null) {
      throw new RuntimeException("Failed to find node sno");
    }

    final Path meshSno = this.root.resolveSibling(meshFile + ".sno");
    final Sno sno = SnoLoader.load(Files.newInputStream(meshSno));
    final SnoRenderer snoRenderer = new SnoRenderer(sno);
    final List<Texture> snoTextures = new ArrayList<>(snoRenderer.textureIndices.size());

    for(final Map.Entry<String, Integer> entry : snoRenderer.textureIndices.entrySet()) {
      snoTextures.add(entry.getValue(), this.textureManager.getTexture(entry.getKey()));
    }

    this.chunk = new SceneChunk(snoRenderer, snoTextures);
    this.cache.put(guid, this.chunk);
  }

  public void draw() {
    this.chunk.draw();
  }

  @Nullable
  private String findMeshFilename(final int guid) {
    for(final SiegeNodes siegeNodes : this.goDb.get(SiegeNodes.class).values()) {
      for(final SiegeNodes.MeshFile meshFile : siegeNodes) {
        if(meshFile.getGuid() == guid) {
          return meshFile.getFilename();
        }
      }
    }

    return null;
  }

  private record SceneChunk(SnoRenderer renderer, List<Texture> textures) {
    public void draw() {
      for(int i = 0; i < this.textures.size(); i++) {
        this.textures.get(i).use(i);
      }

      this.renderer.mesh.draw();
    }
  }
}
