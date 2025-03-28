package lofimodding.opensiege;

import lofimodding.opensiege.formats.gas.GasLoader;
import lofimodding.opensiege.gfx.Context;
import lofimodding.opensiege.gfx.MatrixStack;
import lofimodding.opensiege.gfx.QuaternionCamera;
import lofimodding.opensiege.gfx.Shader;
import lofimodding.opensiege.gfx.Window;
import lofimodding.opensiege.go.GoDb;
import lofimodding.opensiege.world.World;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.opengl.GL11C.GL_FILL;
import static org.lwjgl.opengl.GL11C.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11C.GL_LINE;
import static org.lwjgl.opengl.GL11C.glPolygonMode;

public class Game {
  private static final int WINDOW_WIDTH = 1280;
  private static final int WINDOW_HEIGHT = 720;

  private static final float MOVE_SPEED = 0.16f;
  private static final float MOUSE_SPEED = 0.00175f;

  private final Window window;
  private final Context ctx;

  private final Shader.UniformBuffer transforms2;

  private boolean firstMouse = true;
  private double lastX = WINDOW_WIDTH / 2.0f;
  private double lastY = WINDOW_HEIGHT / 2.0f;
  private float yaw;
  private float pitch;

  private boolean movingLeft;
  private boolean movingRight;
  private boolean movingForward;
  private boolean movingBackward;
  private boolean movingUp;
  private boolean movingDown;

  private boolean wireframeMode;

  private final GoDb goDb;

  public Game(final Path tankRoot, final GoDb goDb, final String mapId, final String regionId, final int nodeGuid) throws IOException {
    this.goDb = goDb;

    this.window = new Window("OpenSiege", WINDOW_WIDTH, WINDOW_HEIGHT);
    this.ctx = new Context(this.window, new QuaternionCamera(0.0f, 3.0f, 10.0f));
    this.ctx.setClearColour(0.0f, 0.0f, 0.0f);

    this.window.events.onMouseMove(this::onMouseMove);
    this.window.events.onKeyPress(this::onKeyPress);
    this.window.events.onKeyRelease(this::onKeyRelease);
    this.window.hideCursor();

    final Shader basicShader = new Shader(Paths.get("gfx/shaders/basic.vsh"), Paths.get("gfx/shaders/basic.fsh"));
    basicShader.bindUniformBlock("transforms", Shader.UniformBuffer.TRANSFORM);
    basicShader.bindUniformBlock("transforms2", Shader.UniformBuffer.TRANSFORM2);

    basicShader.use();
    for(int i = 0; i < 16; i++) {
      basicShader.new UniformInt("tex[" + i + ']').set(i);
    }

    final TextureManager textureManager = new TextureManager(tankRoot);

    final World map = goDb.get(World.class, "map");

    final Path mapPath = tankRoot.resolve("world").resolve("maps").resolve(map.getName());
    final Path regionPath = mapPath.resolve("regions").resolve(regionId);

    goDb.addObject(GasLoader.load(regionPath.resolve("main.gas")));
    goDb.addObject(GasLoader.load(regionPath.resolve("terrain_nodes").resolve("nodes.gas")));

    // Load all siege nodes
    try(final Stream<Path> nodeStream = Files.walk(tankRoot.resolve("world").resolve("global").resolve("siege_nodes"))) {
      nodeStream
        .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".gas"))
        .map(GasLoader::load)
        .forEach(goDb::addObject);
    }

    final FloatBuffer identityBuffer = BufferUtils.createFloatBuffer(4 * 4);
    new Matrix4f().get(identityBuffer);
    this.transforms2 = new Shader.UniformBuffer((long)identityBuffer.capacity() * Float.BYTES, Shader.UniformBuffer.TRANSFORM2);

    final Scene scene = new Scene(tankRoot, textureManager, goDb, this.transforms2);
    scene.setRegion(nodeGuid);

    final MatrixStack matrixStack = new MatrixStack();

    this.ctx.onDraw(() -> {
      // Restore model buffer to identity
      this.transforms2.set(identityBuffer);

      basicShader.use();

      scene.draw(matrixStack);

      if(this.movingLeft) {
        this.ctx.camera.strafe(-MOVE_SPEED);
      }

      if(this.movingRight) {
        this.ctx.camera.strafe(MOVE_SPEED);
      }

      if(this.movingForward) {
        this.ctx.camera.move(MOVE_SPEED);
      }

      if(this.movingBackward) {
        this.ctx.camera.move(-MOVE_SPEED);
      }

      if(this.movingUp) {
        this.ctx.camera.jump(MOVE_SPEED);
      }

      if(this.movingDown) {
        this.ctx.camera.jump(-MOVE_SPEED);
      }
    });

    this.window.show();
    this.window.run();
  }

  private void onMouseMove(final Window window, final double x, final double y) {
    if(this.firstMouse) {
      this.lastX = x;
      this.lastY = y;
      this.firstMouse = false;
    }

    this.yaw   += (x - this.lastX) * MOUSE_SPEED;
    this.pitch += (this.lastY - y) * MOUSE_SPEED;

    this.pitch = (float)Math.max(-Math.PI / 2, Math.min(this.pitch, Math.PI / 2));

    this.lastX = x;
    this.lastY = y;

    this.ctx.camera.look(this.yaw, this.pitch);
  }

  private void onKeyPress(final Window window, final int key, final int scancode, final int mods) {
    switch(key) {
      case GLFW_KEY_W -> this.movingForward = true;
      case GLFW_KEY_S -> this.movingBackward = true;
      case GLFW_KEY_A -> this.movingLeft = true;
      case GLFW_KEY_D -> this.movingRight = true;
      case GLFW_KEY_SPACE -> this.movingUp = true;
      case GLFW_KEY_LEFT_SHIFT -> this.movingDown = true;
      case GLFW_KEY_ESCAPE -> this.window.close();
      case GLFW_KEY_TAB -> {
        this.wireframeMode = !this.wireframeMode;
        glPolygonMode(GL_FRONT_AND_BACK, this.wireframeMode ? GL_LINE : GL_FILL);
      }
    }
  }

  private void onKeyRelease(final Window window, final int key, final int scancode, final int mods) {
    switch(key) {
      case GLFW_KEY_W -> this.movingForward = false;
      case GLFW_KEY_S -> this.movingBackward = false;
      case GLFW_KEY_A -> this.movingLeft = false;
      case GLFW_KEY_D -> this.movingRight = false;
      case GLFW_KEY_SPACE -> this.movingUp = false;
      case GLFW_KEY_LEFT_SHIFT -> this.movingDown = false;
    }
  }
}
