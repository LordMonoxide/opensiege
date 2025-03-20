package lofimodding.opensiege;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lofimodding.opensiege.formats.siegenode.Sno;
import lofimodding.opensiege.formats.siegenode.SnoDoor;
import lofimodding.opensiege.formats.siegenode.SnoLoader;
import lofimodding.opensiege.formats.siegenode.SnoRenderer;
import lofimodding.opensiege.gfx.MatrixStack;
import lofimodding.opensiege.gfx.Shader;
import lofimodding.opensiege.gfx.Texture;
import lofimodding.opensiege.go.GoDb;
import lofimodding.opensiege.world.SiegeNodes;
import lofimodding.opensiege.world.Snode;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Scene {
  private final Path root;
  private final TextureManager textureManager;
  private final GoDb goDb;
  private final Shader.UniformBuffer transforms2;

  private final Int2ObjectMap<SceneChunk> cache = new Int2ObjectOpenHashMap<>();

  private int regionGuid;

  public Scene(final Path root, final TextureManager textureManager, final GoDb goDb, final Shader.UniformBuffer transforms2) {
    this.root = root;
    this.textureManager = textureManager;
    this.goDb = goDb;
    this.transforms2 = transforms2;
  }

  public void setRegion(final int guid) {
    this.regionGuid = guid;
  }

  @Nullable
  private SceneChunk getChunk(final int guid) {
    if(this.cache.containsKey(guid)) {
      return this.cache.get(guid);
    }

    final Snode snode = this.goDb.get(Snode.class, "%#010x".formatted(guid));

    if(snode == null) {
      throw new RuntimeException("Snode %#010x not found".formatted(guid));
    }

    final String meshFile = this.findMeshFilename(snode.getMeshGuid());

    if(meshFile == null) {
      System.err.printf("Failed to find node sno with mesh GUID %#010x%n", snode.getMeshGuid());
      this.cache.put(guid, null);
      return null;
    }

    final Path meshSno = this.root.resolveSibling(meshFile + ".sno");
    final Sno sno;
    try {
      sno = SnoLoader.load(Files.newInputStream(meshSno), snode.getTexSetAbbr());
    } catch(final IOException e) {
      throw new RuntimeException(e);
    }
    final SnoRenderer snoRenderer = new SnoRenderer(sno);
    final Texture[] snoTextures = new Texture[snoRenderer.textureIndices.size()];

    for(final Map.Entry<String, Integer> entry : snoRenderer.textureIndices.entrySet()) {
      snoTextures[entry.getValue()] = this.textureManager.getTexture(entry.getKey());
    }

    final SceneChunk chunk = new SceneChunk(snode, snoRenderer, snoTextures);
    this.cache.put(guid, chunk);
    return chunk;
  }

  private final Set<SceneChunk> visited = new HashSet<>();

  public void draw(final MatrixStack matrixStack) {
    this.visited.clear();
    this.drawChunk(matrixStack, this.getChunk(this.regionGuid), 0);
  }

  private final FloatBuffer identityBuffer = BufferUtils.createFloatBuffer(4 * 4);

  private void drawChunk(final MatrixStack matrixStack, @Nullable final SceneChunk chunk, final int distance) {
    if(this.visited.contains(chunk) || distance > 3000 || chunk == null) {
      return;
    }

    this.visited.add(chunk);

    matrixStack.push();

    this.identityBuffer.clear();
    matrixStack.get(this.identityBuffer);
    this.transforms2.set(this.identityBuffer);

    chunk.draw();

    for(final SnoDoor door : chunk.renderer.sno.doors().values()) {
      final Snode.Door snodeDoor = chunk.snode.getDoor(door.index());

      if(snodeDoor == null) {
        continue;
      }

      matrixStack.push();
      matrixStack.translate(door.translation());
      matrixStack.top().mul(new Matrix4f(door.rotation()));

      final SceneChunk farChunk = this.getChunk(snodeDoor.getFarGuid());

      if(farChunk != null) {
        final SnoDoor farDoor = farChunk.renderer.sno.getDoor(snodeDoor.getFarDoor());

        final Matrix4f farTransforms = new Matrix4f(farDoor.rotation());
        farTransforms.translateLocal(farDoor.translation());
        farTransforms.invert();
        farTransforms.rotateLocalY((float)Math.PI);
        matrixStack.top().mul(farTransforms);

        this.drawChunk(matrixStack, farChunk, distance + 1);
      }

      matrixStack.pop();
    }

    matrixStack.pop();
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

  private record SceneChunk(Snode snode, SnoRenderer renderer, Texture[] textures) {
    public void draw() {
      for(int i = 0; i < this.textures.length; i++) {
        this.textures[i].use(i);
      }

      this.renderer.mesh.draw();
    }
  }
}
