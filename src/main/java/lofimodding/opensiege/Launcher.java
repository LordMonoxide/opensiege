package lofimodding.opensiege;

import java.io.IOException;

public final class Launcher {
  private Launcher() { }

  public static void main(final String[] args) throws IOException {
    System.out.println("OpenSiege start!");

    new Game();
  }
}
