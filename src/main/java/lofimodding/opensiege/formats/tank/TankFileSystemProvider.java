package lofimodding.opensiege.formats.tank;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TankFileSystemProvider extends FileSystemProvider {
  private final Map<String, TankFileSystem> fileSystems = new HashMap<>();

  @Override
  public String getScheme() {
    return "tank";
  }

  @Override
  public TankFileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
    synchronized(this.fileSystems) {
      String schemeSpecificPart = uri.getSchemeSpecificPart();
      final int i = schemeSpecificPart.indexOf("!/");

      if(i >= 0) {
        schemeSpecificPart = schemeSpecificPart.substring(0, i);
      }

      TankFileSystem fileSystem = this.fileSystems.get(schemeSpecificPart);

      if(fileSystem != null) {
        throw new FileSystemAlreadyExistsException(schemeSpecificPart);
      }

      fileSystem = new TankFileSystem(this, new TankManager(Paths.get(URLDecoder.decode(schemeSpecificPart, StandardCharsets.UTF_8))), env);
      this.fileSystems.put(schemeSpecificPart, fileSystem);
      return fileSystem;
    }
  }

  @Override
  public TankFileSystem getFileSystem(final URI uri) {
    return this.getFileSystem(uri, false);
  }

  public TankFileSystem getFileSystem(final URI uri, final boolean create) {
    synchronized(this.fileSystems) {
      String schemeSpecificPart = uri.getSchemeSpecificPart();
      final int i = schemeSpecificPart.indexOf("!/");

      if(i >= 0) {
        schemeSpecificPart = schemeSpecificPart.substring(0, i);
      }

      TankFileSystem fileSystem = this.fileSystems.get(schemeSpecificPart);

      if(fileSystem == null) {
        if(create) {
          try {
            fileSystem = this.newFileSystem(uri, null);
          } catch(final IOException e) {
            throw (FileSystemNotFoundException)new FileSystemNotFoundException(schemeSpecificPart).initCause(e);
          }
        } else {
          throw new FileSystemNotFoundException(schemeSpecificPart);
        }
      }

      return fileSystem;
    }
  }

  @Override
  public Path getPath(final URI uri) {
    final String str = uri.getSchemeSpecificPart();
    final int i = str.indexOf("!/");

    if(i == -1) {
      throw new IllegalArgumentException("URI: " + uri + " does not contain path info ex. github:apache/karaf#master!/");
    }

    return this.getFileSystem(uri, true).getPath(str.substring(i + 1));
  }

  @Override
  public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) throws IOException {
    if(!(path instanceof TankPath)) {
      throw new ProviderMismatchException();
    }

    return ((TankPath)path).getFileSystem().newByteChannel(path, options, attrs);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(final Path dir, final DirectoryStream.Filter<? super Path> filter) throws IOException {
    if(!(dir instanceof TankPath)) {
      throw new ProviderMismatchException();
    }
    return ((TankPath)dir).getFileSystem().newDirectoryStream(dir, filter);
  }

  @Override
  public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {
    if(!(path instanceof TankPath)) {
      throw new ProviderMismatchException();
    }
    return ((TankPath)path).getFileSystem().newInputStream(path, options);
  }

  @Override
  public void createDirectory(final Path dir, final FileAttribute<?>... attrs) {
    throw new ReadOnlyFileSystemException();
  }

  @Override
  public void delete(final Path path) {
    throw new ReadOnlyFileSystemException();
  }

  @Override
  public void copy(final Path source, final Path target, final CopyOption... options) {
    throw new ReadOnlyFileSystemException();
  }

  @Override
  public void move(final Path source, final Path target, final CopyOption... options) {
    throw new ReadOnlyFileSystemException();
  }

  @Override
  public boolean isSameFile(final Path path, final Path path2) {
    return path.toAbsolutePath().equals(path2.toAbsolutePath());
  }

  @Override
  public boolean isHidden(final Path path) {
    return false;
  }

  @Override
  public FileStore getFileStore(final Path path) {
    return null;
  }

  @Override
  public void checkAccess(final Path path, final AccessMode... modes) {

  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type, final LinkOption... options) {
    return null;
  }

  @Override
  public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type, final LinkOption... options) throws IOException {
    if(!(path instanceof TankPath)) {
      throw new ProviderMismatchException();
    }
    return ((TankPath)path).getFileSystem().readAttributes(path, type, options);
  }

  @Override
  public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options) {
    return null;
  }

  @Override
  public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options) {
    throw new ReadOnlyFileSystemException();
  }
}
