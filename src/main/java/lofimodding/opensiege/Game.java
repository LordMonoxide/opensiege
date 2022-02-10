package lofimodding.opensiege;

import lofimodding.opensiege.gfx.Context;
import lofimodding.opensiege.gfx.QuaternionCamera;
import lofimodding.opensiege.gfx.Window;

import java.io.IOException;

public class Game {
  private static final int WINDOW_WIDTH = 1280;
  private static final int WINDOW_HEIGHT = 720;

  private final Window window;
  private final Context ctx;

  public Game() throws IOException {
    this.window = new Window("Town Watch", WINDOW_WIDTH, WINDOW_HEIGHT);
    this.ctx = new Context(this.window, new QuaternionCamera(0.0f, 3.0f, 10.0f));
    this.ctx.setClearColour(135.0f / 255.0f, 206.0f / 255.0f, 235.0f / 255.0f);

    this.ctx.onDraw(() -> {
    });

    this.window.show();
    this.window.run();
  }
}
