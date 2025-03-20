package lofimodding.opensiege;

import lofimodding.opensiege.formats.raw.RawTexture;
import lofimodding.opensiege.formats.raw.RawTextureLoader;
import lofimodding.opensiege.gfx.Texture;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_NEAREST_MIPMAP_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12C.GL_BGRA;

public class TextureManager {
  private final Path root;

  //TODO remove textures from cache after x seconds
  private final Map<String, Texture> cache = new HashMap<>();

  public TextureManager(final Path root) {
    this.root = root;
  }

  public Texture getTexture(final String name) {
    final Texture existing = this.cache.get(name);

    if(existing != null) {
      return existing;
    }

    try {
      final RawTexture raw = RawTextureLoader.load(Files.newInputStream(this.root.resolveSibling(name + ".raw")));

      final Texture texture = Texture.create(builder -> {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(raw.data()[0].length);
        buffer.put(raw.data()[0]);
        buffer.flip();

        builder.data(buffer, raw.header().width(), raw.header().height());
        builder.dataFormat(GL_BGRA);
        builder.dataType(GL_UNSIGNED_BYTE);
        builder.minFilter(GL_LINEAR_MIPMAP_LINEAR);

        for(int i = 1; i < raw.header().surfaceCount(); i++) {
          final int finalI = i;
          builder.mipmap(i, mipmap -> {
            final ByteBuffer mipBuffer = BufferUtils.createByteBuffer(raw.data()[finalI].length);
            mipBuffer.put(raw.data()[finalI]);
            mipBuffer.flip();

            final int divisor = 1 << finalI;
            mipmap.data(mipBuffer, raw.header().width() / divisor, raw.header().height() / divisor);
            mipmap.dataFormat(GL_BGRA);
            mipmap.dataType(GL_UNSIGNED_BYTE);
          });
        }
      });

      this.cache.put(name, texture);
      return texture;
    } catch(final IOException e) {
      System.err.println("Failed to load texture " + name + " - " + e.getLocalizedMessage());

      final Texture texture = Texture.empty(1, 1);
      this.cache.put(name, texture);
      return texture;
    }
  }
}
