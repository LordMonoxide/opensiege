package lofimodding.opensiege;

import lofimodding.opensiege.formats.aspect.Aspect;
import lofimodding.opensiege.formats.aspect.AspectLoader;
import lofimodding.opensiege.formats.tank.Tank;
import lofimodding.opensiege.formats.tank.TankLoader;
import lofimodding.opensiege.gfx.Context;
import lofimodding.opensiege.gfx.Mesh;
import lofimodding.opensiege.gfx.QuaternionCamera;
import lofimodding.opensiege.gfx.Shader;
import lofimodding.opensiege.gfx.Window;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Paths;

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

  public Game() throws IOException {
    this.window = new Window("Town Watch", WINDOW_WIDTH, WINDOW_HEIGHT);
    this.ctx = new Context(this.window, new QuaternionCamera(0.0f, 3.0f, 10.0f));
    this.ctx.setClearColour(0.0f, 0.0f, 0.0f);

    this.window.events.onMouseMove(this::onMouseMove);
    this.window.events.onKeyPress(this::onKeyPress);
    this.window.events.onKeyRelease(this::onKeyRelease);
    this.window.hideCursor();

    final Shader basicShader = new Shader(Paths.get("gfx/shaders/basic.vsh"), Paths.get("gfx/shaders/basic.fsh"));
    basicShader.bindUniformBlock("transforms", Shader.UniformBuffer.TRANSFORM);
    basicShader.bindUniformBlock("transforms2", Shader.UniformBuffer.TRANSFORM2);

    final float[] vertices = {
      -0.5f, -0.5f,  0.5f,   0.0f,  1.0f, // bottom-left
       0.5f, -0.5f,  0.5f,   1.0f,  1.0f, // bottom-right
       0.5f,  0.5f,  0.5f,   1.0f,  0.0f, // top-right
       0.5f,  0.5f,  0.5f,   1.0f,  0.0f, // top-right
      -0.5f,  0.5f,  0.5f,   0.0f,  0.0f, // top-left
      -0.5f, -0.5f,  0.5f,   0.0f,  1.0f, // bottom-left
    };

//    final Mesh mesh = new Mesh(GL_TRIANGLES, vertices, 6);
//    mesh.attribute(0, 0L, 3, 5);
//    mesh.attribute(1, 3L, 2, 5);

    final Tank tank = TankLoader.load(Paths.get("C:", "Program Files (x86)", "Steam", "steamapps", "common", "Dungeon Siege 1", "Resources", "Objects.dsres"));

    final InputStream aspectData = tank.getFile("/art/meshes/gui/front_end/menus/main/m_gui_fe_m_mn_3d_logo.asp");
    final Aspect aspect = AspectLoader.load(aspectData);

    final FloatBuffer identityBuffer = BufferUtils.createFloatBuffer(4 * 4);
    new Matrix4f().get(identityBuffer);
    this.transforms2 = new Shader.UniformBuffer((long)identityBuffer.capacity() * Float.BYTES, Shader.UniformBuffer.TRANSFORM2);

    this.ctx.onDraw(() -> {
      // Restore model buffer to identity
      this.transforms2.set(identityBuffer);

      basicShader.use();
//      mesh.draw();

      for(final Mesh mesh : aspect.meshes()) {
        mesh.draw();
      }

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
