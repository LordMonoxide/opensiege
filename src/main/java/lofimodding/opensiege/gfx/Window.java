package lofimodding.opensiege.gfx;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
  private static final GLFWErrorCallback ERROR_CALLBACK;

  static {
    System.out.println("Initialising LWJGL version " + Version.getVersion());

    ERROR_CALLBACK = GLFWErrorCallback.createPrint(System.err).set();

    if(!glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Terminating");

      glfwTerminate();
      ERROR_CALLBACK.free();
    }));
  }

  private final long window;

  private int width;
  private int height;

  public final Events events = new Events(this);

  public Window(final String title, final int width, final int height) {
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

    this.window = glfwCreateWindow(width, height, title, NULL, NULL);
    this.width = width;
    this.height = height;

    if(this.window == NULL) {
      throw new RuntimeException("Failed to create the GLFW window");
    }

    glfwSetWindowSizeCallback(this.window, this.events::onResize);
    glfwSetKeyCallback(this.window, this.events::onKey);
    glfwSetCursorPosCallback(this.window, this.events::onMouseMove);

    try(final MemoryStack stack = stackPush()) {
      final IntBuffer pWidth = stack.mallocInt(1);
      final IntBuffer pHeight = stack.mallocInt(1);

      glfwGetWindowSize(this.window, pWidth, pHeight);

      final GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

      if(vidMode == null) {
        throw new RuntimeException("Failed to get video mode");
      }

      glfwSetWindowPos(
        this.window,
        (vidMode.width() - pWidth.get(0)) / 2,
        (vidMode.height() - pHeight.get(0)) / 2
      );
    }
  }

  void makeContextCurrent() {
    glfwMakeContextCurrent(this.window);
    glfwSwapInterval(1);
  }

  public void show() {
    glfwShowWindow(this.window);
    this.events.onResize(this.window, this.width, this.height);
  }

  public void close() {
    glfwSetWindowShouldClose(this.window, true);
  }

  public void hideCursor() {
    glfwSetInputMode(this.window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
  }

  public void showCursor() {
    glfwSetInputMode(this.window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
  }

  public void run() {
    while(!glfwWindowShouldClose(this.window)) {
      this.events.onDraw();

      glfwSwapBuffers(this.window);
      glfwPollEvents();
    }

    glfwFreeCallbacks(this.window);
    glfwDestroyWindow(this.window);
  }

  public static final class Events {
    private final List<Resize> resize = new ArrayList<>();
    private final List<Key> keyPress = new ArrayList<>();
    private final List<Key> keyRelease = new ArrayList<>();
    private final List<Key> keyRepeat = new ArrayList<>();
    private final List<Cursor> mouseMove = new ArrayList<>();
    private final List<Runnable> draw = new ArrayList<>();
    private final Window window;

    private Events(final Window window) {
      this.window = window;
    }

    private void onResize(final long window, final int width, final int height) {
      this.window.width = width;
      this.window.height = height;
      this.resize.forEach(cb -> cb.resize(this.window, width, height));
    }

    public void onResize(final Resize callback) {
      this.resize.add(callback);
    }

    private void onKey(final long window, final int key, final int scancode, final int action, final int mods) {
      switch(action) {
        case GLFW_PRESS -> this.keyPress.forEach(cb -> cb.action(this.window, key, scancode, mods));
        case GLFW_RELEASE -> this.keyRelease.forEach(cb -> cb.action(this.window, key, scancode, mods));
        case GLFW_REPEAT -> this.keyRepeat.forEach(cb -> cb.action(this.window, key, scancode, mods));
      }
    }

    private void onMouseMove(final long window, final double x, final double y) {
      final double newY = this.window.height - y;
      this.mouseMove.forEach(cb -> cb.action(this.window, x, newY));
    }

    public void onKeyPress(final Key callback) {
      this.keyPress.add(callback);
    }

    public void onKeyRelease(final Key callback) {
      this.keyRelease.add(callback);
    }

    public void onKeyRepeat(final Key callback) {
      this.keyRepeat.add(callback);
    }

    public void onMouseMove(final Cursor callback) {
      this.mouseMove.add(callback);
    }

    private void onDraw() {
      for(final Runnable draw : this.draw) {
        draw.run();
      }
    }

    public void onDraw(final Runnable callback) {
      this.draw.add(callback);
    }

    @FunctionalInterface public interface Resize {
      void resize(final Window window, final int width, final int height);
    }

    @FunctionalInterface public interface Key {
      void action(final Window window, final int key, final int scancode, final int mods);
    }

    @FunctionalInterface public interface Cursor {
      void action(final Window window, final double x, final double y);
    }
  }
}
