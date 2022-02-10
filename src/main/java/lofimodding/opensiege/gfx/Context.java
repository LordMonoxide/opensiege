package lofimodding.opensiege.gfx;

import lofimodding.opensiege.gfx.lights.DirectionalLight;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Paths;
import java.util.Random;

import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;
import static org.lwjgl.opengl.GL11C.GL_RED;
import static org.lwjgl.opengl.GL11C.GL_REPEAT;
import static org.lwjgl.opengl.GL11C.GL_RGB;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11C.GL_VENDOR;
import static org.lwjgl.opengl.GL11C.GL_VERSION;
import static org.lwjgl.opengl.GL11C.glClear;
import static org.lwjgl.opengl.GL11C.glClearColor;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL11C.glGetString;
import static org.lwjgl.opengl.GL11C.glViewport;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT1;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT2;
import static org.lwjgl.opengl.GL30C.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30C.GL_RGB16F;
import static org.lwjgl.opengl.GL30C.GL_RGB32F;

public class Context {
  private final Window window;

  public final Shader gBufferTextureShader;
  public final Shader gBufferFlatShader;
  private final Shader gShadingShader;
  private FrameBuffer gBuffer;
  private Texture gPosition;
  private Texture gNormal;
  private Texture gColour;
  private final Mesh gQuad;

  private static final int SSAO_KERNEL_SIZE = 64;
  private static final int SSAO_NOISE_SIZE = 4;

  private final Shader ssaoShader;
  private FrameBuffer ssaoBuffer;
  private Texture ssaoTexture;
  private final Texture ssaoNoiseTexture;

  private final Shader ssaoBlurShader;
  private FrameBuffer ssaoBlurBuffer;
  private Texture ssaoBlurTexture;

  private final Shader.UniformVec3 sunDirection;
  private final Shader.UniformVec3 sunAmbient;
  private final Shader.UniformVec3 sunDiffuse;
  private final Shader.UniformVec3 sunSpecular;

  public final Camera camera;
  private final Matrix4f proj = new Matrix4f();
  private final Shader.UniformBuffer transforms;
  private final Shader.UniformBuffer ssaoUniform;
  private final Shader.UniformBuffer lighting;

  private final FloatBuffer transformsBuffer = BufferUtils.createFloatBuffer(4 * 4 * 2);
  private final FloatBuffer ssaoUniformBuffer = BufferUtils.createFloatBuffer(4);
  private final FloatBuffer lightingBuffer = BufferUtils.createFloatBuffer(4);

  private final Vector4f ssaoNoiseScale = new Vector4f();

  private Runnable onDraw = () -> {};

  private int width;
  private int height;

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
    glEnable(GL_CULL_FACE);

    this.setClearColour(0.0f, 0.0f, 0.0f);

    this.gBufferTextureShader = new Shader(Paths.get("gfx/shaders/g_buffer_tex.vsh"), Paths.get("gfx/shaders/g_buffer_tex.fsh"));
    this.gBufferTextureShader.bindUniformBlock("transforms", Shader.UniformBuffer.TRANSFORM);
    this.gBufferTextureShader.bindUniformBlock("transforms2", Shader.UniformBuffer.TRANSFORM2);

    this.gBufferFlatShader = new Shader(Paths.get("gfx/shaders/g_buffer_flat.vsh"), Paths.get("gfx/shaders/g_buffer_flat.fsh"));
    this.gBufferFlatShader.bindUniformBlock("transforms", Shader.UniformBuffer.TRANSFORM);
    this.gBufferFlatShader.bindUniformBlock("transforms2", Shader.UniformBuffer.TRANSFORM2);

    this.gShadingShader = new Shader(Paths.get("gfx/shaders/deferred_shading.vsh"), Paths.get("gfx/shaders/deferred_shading.fsh"));
    this.gShadingShader.bindUniformBlock("lighting", Shader.UniformBuffer.LIGHTING);
    this.gShadingShader.use();
    this.gShadingShader.new UniformInt("gPosition").set(0);
    this.gShadingShader.new UniformInt("gNormal").set(1);
    this.gShadingShader.new UniformInt("gColour").set(2);
    this.gShadingShader.new UniformInt("ssao").set(3);

    this.sunDirection = this.gShadingShader.new UniformVec3("directionalLight.direction");
    this.sunAmbient = this.gShadingShader.new UniformVec3("directionalLight.ambient");
    this.sunDiffuse = this.gShadingShader.new UniformVec3("directionalLight.diffuse");
    this.sunSpecular = this.gShadingShader.new UniformVec3("directionalLight.specular");

    this.gQuad = new Mesh(GL_TRIANGLES, new float[] {
      -1.0f, -1.0f,  0.0f, 0.0f,
       1.0f, -1.0f,  1.0f, 0.0f,
      -1.0f,  1.0f,  0.0f, 1.0f,
       1.0f,  1.0f,  1.0f, 1.0f,
    }, new int[] {
      0, 3, 2,
      0, 1, 3,
    });

    this.gQuad.attribute(0, 0L, 2, 4);
    this.gQuad.attribute(1, 2L, 2, 4);

    this.ssaoShader = new Shader(Paths.get("gfx/shaders/deferred_shading.vsh"), Paths.get("gfx/shaders/ssao.fsh"));
    this.ssaoShader.bindUniformBlock("transforms", Shader.UniformBuffer.TRANSFORM);
    this.ssaoShader.bindUniformBlock("ssao", Shader.UniformBuffer.SSAO);
    this.ssaoShader.use();
    this.ssaoShader.new UniformInt("gPosition").set(0);
    this.ssaoShader.new UniformInt("gNormal").set(1);
    this.ssaoShader.new UniformInt("texNoise").set(2);
    this.ssaoShader.new UniformInt("kernelSize").set(64);

    final Random rand = new Random();

    for(int i = 0; i < SSAO_KERNEL_SIZE; i++) {
      final float s = (float)i / SSAO_KERNEL_SIZE;
      final float scale = this.lerp(0.1f, 1.0f, s * s);

      this.ssaoShader.new UniformVec3("samples[" + i + ']').set(new Vector3f(
        rand.nextFloat() * 2.0f - 1.0f,
        rand.nextFloat() * 2.0f - 1.0f,
        rand.nextFloat()
      )
        .normalize()
        .mul(rand.nextFloat())
        // scale samples so that they're more aligned to center of kernel
        .mul(scale));
    }

    final FloatBuffer ssaoNoise = BufferUtils.createFloatBuffer(16 * 3);

    for(int i = 0; i < SSAO_NOISE_SIZE * SSAO_NOISE_SIZE; i++) {
      ssaoNoise.put(rand.nextFloat() * 2.0f - 1.0f);
      ssaoNoise.put(rand.nextFloat() * 2.0f - 1.0f);
      ssaoNoise.put(0.0f);
    }

    ssaoNoise.flip();

    this.ssaoNoiseTexture = Texture.create(texture -> {
      texture.data(ssaoNoise, SSAO_NOISE_SIZE, SSAO_NOISE_SIZE);
      texture.internalFormat(GL_RGB32F);
      texture.dataType(GL_FLOAT);
      texture.dataFormat(GL_RGB);
      texture.wrapS(GL_REPEAT);
      texture.wrapT(GL_REPEAT);
    });

    this.ssaoBlurShader = new Shader(Paths.get("gfx/shaders/deferred_shading.vsh"), Paths.get("gfx/shaders/ssao_blur.fsh"));
    this.ssaoBlurShader.use();
    this.ssaoBlurShader.new UniformInt("ssaoInput").set(0);

    this.transforms = new Shader.UniformBuffer((long)this.transformsBuffer.capacity() * Float.BYTES, Shader.UniformBuffer.TRANSFORM);
    this.ssaoUniform = new Shader.UniformBuffer((long)this.ssaoUniformBuffer.capacity() * Float.BYTES, Shader.UniformBuffer.SSAO);
    this.lighting = new Shader.UniformBuffer(4L * 4L, Shader.UniformBuffer.LIGHTING);
  }

  private float lerp(final float a, final float b, final float f) {
    return a + f * (b - a);
  }

  public void useSun(final DirectionalLight sun) {
    this.gShadingShader.use();
    sun.use(this.sunDirection, this.sunAmbient, this.sunDiffuse, this.sunSpecular);
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
    //TODO: for some reason it's extremely laggy on my Intel UHD620 with vsync enabled... but this fixes it
    //      https://discourse.glfw.org/t/swap-interval-n-cause-1-n-fps-except-if-glgeterror-is-called/1298/3
    // glGetError();

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

    // Update lighting
    this.camera.getPos(this.lightingBuffer);
    this.lighting.set(this.lightingBuffer);

    // Render scene to g-buffer
    this.gBuffer.bind();
    this.clear();
  }

  private void post() {
    // SSAO
    this.ssaoBuffer.bind();
    this.clear();

    this.ssaoShader.use();
    this.gPosition.use(0);
    this.gNormal.use(1);
    this.ssaoNoiseTexture.use(2);

    this.gQuad.draw();

    // SSAO blur
    this.ssaoBlurBuffer.bind();
    this.clearColour();

    this.ssaoBlurShader.use();
    this.ssaoTexture.use(0);

    this.gQuad.draw();

    FrameBuffer.unbind();

    // Perform deferred shading
    this.clear();
    this.gShadingShader.use();
    this.gPosition.use(0);
    this.gNormal.use(1);
    this.gColour.use(2);
    this.ssaoBlurTexture.use(3);

    this.gQuad.draw();
  }

  private void onResize(final Window window, final int width, final int height) {
    if(width == 0 && height == 0) {
      return;
    }

    this.width = width;
    this.height = height;

    this.ssaoNoiseScale.x = (float)width / SSAO_NOISE_SIZE;
    this.ssaoNoiseScale.y = (float)height / SSAO_NOISE_SIZE;
    this.ssaoNoiseScale.get(this.ssaoUniformBuffer);
    this.ssaoUniform.set(this.ssaoUniformBuffer);

    this.proj.setPerspective((float)Math.toRadians(45.0f), (float)width / height, 0.1f, 500.0f);

    this.gPosition = Texture.create(texture -> {
      texture.size(width, height);
      texture.internalFormat(GL_RGB16F);
      texture.dataType(GL_FLOAT);
    });

    this.gNormal = Texture.create(texture -> {
      texture.size(width, height);
      texture.internalFormat(GL_RGB16F);
      texture.dataType(GL_FLOAT);
    });

    this.gColour = Texture.create(texture -> {
      texture.size(width, height);
      texture.internalFormat(GL_RGBA);
      texture.dataFormat(GL_RGBA);
    });

    this.gBuffer = FrameBuffer.create(fb -> {
      fb.attachment(this.gPosition, GL_COLOR_ATTACHMENT0);
      fb.attachment(this.gNormal, GL_COLOR_ATTACHMENT1);
      fb.attachment(this.gColour, GL_COLOR_ATTACHMENT2);
      fb.drawBuffer(GL_COLOR_ATTACHMENT0);
      fb.drawBuffer(GL_COLOR_ATTACHMENT1);
      fb.drawBuffer(GL_COLOR_ATTACHMENT2);
      fb.renderBuffer(GL_DEPTH_COMPONENT, GL_DEPTH_ATTACHMENT, width, height);
    });

    this.ssaoTexture = Texture.create(texture -> {
      texture.size(width, height);
      texture.internalFormat(GL_RED);
      texture.dataType(GL_FLOAT);
      texture.dataFormat(GL_RGB);
    });

    this.ssaoBuffer = FrameBuffer.create(fb -> {
      fb.attachment(this.ssaoTexture, GL_COLOR_ATTACHMENT0);
    });

    this.ssaoBlurTexture = Texture.create(texture -> {
      texture.size(width, height);
      texture.internalFormat(GL_RED);
      texture.dataType(GL_FLOAT);
      texture.dataFormat(GL_RGB);
    });

    this.ssaoBlurBuffer = FrameBuffer.create(fb -> {
      fb.attachment(this.ssaoBlurTexture, GL_COLOR_ATTACHMENT0);
    });
  }
}
