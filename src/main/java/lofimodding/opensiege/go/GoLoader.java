package lofimodding.opensiege.go;

import lofimodding.opensiege.formats.gas.GasEntry;

public interface GoLoader<T extends GameObject> {
  T load(final String name, final GasEntry root);
}
