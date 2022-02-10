package lofimodding.opensiege;

import lofimodding.opensiege.formats.aspect.Aspect;
import lofimodding.opensiege.formats.aspect.AspectLoader;
import lofimodding.opensiege.formats.tank.Tank;
import lofimodding.opensiege.formats.tank.TankLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

public final class Launcher {
  private Launcher() { }

  public static void main(final String[] args) throws IOException {
    System.out.println("OpenSiege start!");

    final Tank tank = TankLoader.load(Paths.get("C:", "Program Files (x86)", "Steam", "steamapps", "common", "Dungeon Siege 1", "Resources", "Objects.dsres"));

    final InputStream aspectData = tank.getFile("/art/meshes/gui/front_end/menus/main/m_gui_fe_m_mn_3d_logo.asp");
    final Aspect aspect = AspectLoader.load(aspectData);

    final Game game = new Game();
  }
}
