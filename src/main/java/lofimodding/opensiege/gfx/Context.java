package lofimodding.opensiege.gfx;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_LESS;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11C.GL_VENDOR;
import static org.lwjgl.opengl.GL11C.GL_VERSION;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glClearColor;
import static org.lwjgl.opengl.GL11C.glDepthFunc;
import static org.lwjgl.opengl.GL11C.glDepthMask;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glGetString;
import static org.lwjgl.opengl.GL11C.glViewport;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;

public class Context {
  private final Window window;

  private final Mesh postQuad;
  private final Shader postShader;
  private FrameBuffer postBuffer;
  private Texture postTexture;
  private Texture depthTexture;

  public final Camera camera;
  private final Matrix4f proj = new Matrix4f();
  private final Shader.UniformBuffer transforms;

  private final FloatBuffer transformsBuffer = BufferUtils.createFloatBuffer(4 * 4 * 2);

  private Runnable onDraw = () -> {};

  private int width;
  private int height;

  /** Set when resizing the window so that the render buffers will be resized on the next frame */
  private boolean resizeRenderBuffers;

  public Context(final Window window, final Camera camera) throws IOException {
    this.window = window;
    this.window.events.onResize(this::onResize);
    this.window.events.onDraw(this::draw);
    this.camera = camera;

    window.makeContextCurrent();

    GL.createCapabilities();

    System.out.println("OpenGL version: " + glGetString(GL_VERSION));
    System.out.println("GLSL version: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
    System.out.println("Device manufacturer: " + glGetString(GL_VENDOR));

    glEnable(GL_DEPTH_TEST);
    glDepthMask(true);
    glDepthFunc(GL_LESS);

    glEnable(GL_CULL_FACE);

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    this.setClearColour(0.0f, 0.0f, 0.0f);

    this.postShader = new Shader(Paths.get("gfx/shaders/post.vsh"), Paths.get("gfx/shaders/post.fsh"));

    this.postQuad = new Mesh(GL_TRIANGLES, new float[] {
      -1.0f, -1.0f,  0.0f, 0.0f,
       1.0f, -1.0f,  1.0f, 0.0f,
      -1.0f,  1.0f,  0.0f, 1.0f,
       1.0f,  1.0f,  1.0f, 1.0f,
    }, new int[] {
      0, 3, 2,
      0, 1, 3,
    });

    this.postQuad.attribute(0, 0L, 2, 4);
    this.postQuad.attribute(1, 2L, 2, 4);

    this.transforms = new Shader.UniformBuffer((long)this.transformsBuffer.capacity() * Float.BYTES, Shader.UniformBuffer.TRANSFORM);
  }

  public void setClearColour(final float red, final float green, final float blue) {
    glClearColor(red, green, blue, 1.0f);
  }

  public void onDraw(final Runnable onDraw) {
    this.onDraw = onDraw;
  }

  public void clear() {
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
  }

  public void clearColour() {
    glClear(GL_COLOR_BUFFER_BIT);
  }

  private void draw() {
    if(this.resizeRenderBuffers) {
      this.resizeRenderBuffers = false;
      this.resizeRenderBuffers();
    }

    glViewport(0, 0, this.width, this.height);
    this.pre();
    this.onDraw.run();
    this.post();
  }

  private void pre() {
    // Update global transforms
    this.camera.get(this.transformsBuffer);
    this.proj.get(16, this.transformsBuffer);
    this.transforms.set(this.transformsBuffer);

    // Render scene to g-buffer
    this.postBuffer.bind();
    this.clear();
  }

  private void post() {
    FrameBuffer.unbind();

    // Gamma correction
    this.clear();
    this.postShader.use();
    this.postTexture.use();
    this.postQuad.draw();
  }

  private void onResize(final Window window, final int width, final int height) {
    if(width == 0 && height == 0) {
      return;
    }

    this.width = width;
    this.height = height;

    this.proj.setPerspective((float)Math.toRadians(45.0f), (float)width / height, 0.1f, 500.0f);

    this.resizeRenderBuffers = true;
  }

  private void resizeRenderBuffers() {
    if(this.postTexture != null) {
      this.postTexture.delete();
    }

    this.postTexture = Texture.create(texture -> {
      texture.size(this.width, this.height);
      texture.internalFormat(GL_RGB);
      texture.dataFormat(GL_RGB);
      texture.dataType(GL_UNSIGNED_BYTE);
    });

    if(this.depthTexture != null) {
      this.depthTexture.delete();
    }

    this.depthTexture = Texture.create(texture -> {
      texture.size(this.width, this.height);
      texture.internalFormat(GL_DEPTH_COMPONENT);
      texture.dataFormat(GL_DEPTH_COMPONENT);
      texture.dataType(GL_FLOAT);
    });

    if(this.postBuffer != null) {
      this.postBuffer.delete();
    }

    this.postBuffer = FrameBuffer.create(fb -> {
      fb.attachment(this.postTexture, GL_COLOR_ATTACHMENT0);
      fb.attachment(this.depthTexture, GL_DEPTH_ATTACHMENT);
    });
  }
}
