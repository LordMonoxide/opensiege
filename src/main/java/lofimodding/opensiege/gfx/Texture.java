package lofimodding.opensiege.gfx;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.GL_REPEAT;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glGenTextures;
import static org.lwjgl.opengl.GL11C.glTexImage2D;
import static org.lwjgl.opengl.GL11C.glTexParameteri;
import static org.lwjgl.opengl.GL12C.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL13C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13C.glActiveTexture;
import static org.lwjgl.opengl.GL21C.GL_SRGB_ALPHA;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public class Texture {
  public static Texture create(final Consumer<Builder> callback) {
    final Builder builder = new Builder();

    callback.accept(builder);
    return builder.build();
  }

  public static Texture empty(final int w, final int h) {
    return Texture.create(builder -> {
      builder.size(w, h);
      builder.internalFormat(GL_RGB);
      builder.dataFormat(GL_RGB);
      builder.minFilter(GL_NEAREST);
      builder.magFilter(GL_NEAREST);
    });
  }

  final int id;

  public final int width;
  public final int height;

  private Texture(@Nullable final ByteBuffer data, @Nullable final FloatBuffer dataF, final int w, final int h, final int internalFormat, final int dataFormat, final int dataType, final int minFilter, final int magFilter, final int wrapS, final int wrapT, final boolean generateMipmaps, final List<MipmapBuilder> mipmaps) {
    this.id = glGenTextures();
    this.width = w;
    this.height = h;
    this.use();

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magFilter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapS);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapT);

    if(dataF != null) {
      glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, w, h, 0, dataFormat, dataType, dataF);
    } else {
      glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, w, h, 0, dataFormat, dataType, data);
    }

    if(generateMipmaps) {
      glGenerateMipmap(GL_TEXTURE_2D);
    } else {
      if(!mipmaps.isEmpty()) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, mipmaps.size());
        mipmaps.forEach(MipmapBuilder::use);
      }
    }
  }

  public void use(final int activeTexture) {
    glActiveTexture(GL_TEXTURE0 + activeTexture);
    glBindTexture(GL_TEXTURE_2D, this.id);
  }

  public void use() {
    this.use(0);
  }

  public static class Builder {
    @Nullable
    private ByteBuffer data;
    @Nullable
    private FloatBuffer dataF;
    private int w, h;

    private int internalFormat = GL_SRGB_ALPHA;
    private int dataFormat = GL_RGB;
    private int dataType = GL_UNSIGNED_BYTE;

    private int minFilter = GL_NEAREST;
    private int magFilter = GL_NEAREST;

    private int wrapS = GL_REPEAT;
    private int wrapT = GL_REPEAT;

    private boolean generateMipmaps;
    private final List<MipmapBuilder> mipmaps = new ArrayList<>();

    Builder() {}

    public void size(final int w, final int h) {
      this.w = w;
      this.h = h;
    }

    public void data(@Nullable final ByteBuffer data, final int w, final int h) {
      this.data = data;
      this.size(w, h);
    }

    public void data(@Nullable final FloatBuffer data, final int w, final int h) {
      this.dataF = data;
      this.size(w, h);
    }

    public void internalFormat(final int format) {
      this.internalFormat = format;
    }

    public void dataFormat(final int format) {
      this.dataFormat = format;
    }

    public void dataType(final int dataType) {
      this.dataType = dataType;
    }

    public void minFilter(final int minFilter) {
      this.minFilter = minFilter;
    }

    public void magFilter(final int magFilter) {
      this.magFilter = magFilter;
    }

    public void wrapS(final int wrapS) {
      this.wrapS = wrapS;
    }

    public void wrapT(final int wrapT) {
      this.wrapT = wrapT;
    }

    public void generateMipmaps() {
      this.generateMipmaps = true;
    }

    public void mipmap(final int level, final Consumer<MipmapBuilder> callback) {
      final MipmapBuilder builder = new MipmapBuilder(level);

      callback.accept(builder);
      this.mipmaps.add(builder);
    }

    Texture build() {
      return new Texture(this.data, this.dataF, this.w, this.h, this.internalFormat, this.dataFormat, this.dataType, this.minFilter, this.magFilter, this.wrapS, this.wrapT, this.generateMipmaps, this.mipmaps);
    }
  }

  public static class MipmapBuilder {
    private final int level;

    @Nullable
    private ByteBuffer data;
    private int w, h;

    private int dataFormat = GL_SRGB_ALPHA;
    private int pixelFormat = GL_RGB;
    private int dataType = GL_UNSIGNED_BYTE;

    MipmapBuilder(final int level) {
      this.level = level;
    }

    public void data(@Nullable final ByteBuffer data, final int w, final int h) {
      this.data = data;
      this.w = w;
      this.h = h;
    }

    public void dataFormat(final int format) {
      this.dataFormat = format;
    }

    public void pixelFormat(final int format) {
      this.pixelFormat = format;
    }

    public void dataType(final int dataType) {
      this.dataType = dataType;
    }

    void use() {
      glTexImage2D(GL_TEXTURE_2D, this.level, this.dataFormat, this.w, this.h, 0, this.pixelFormat, this.dataType, this.data);
    }
  }
}
