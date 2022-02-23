package lofimodding.opensiege.formats.tank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TankFileSystem extends FileSystem {
  private final TankFileSystemProvider fileSystemProvider;
  private final TankManager tankManager;

  public TankFileSystem(final TankFileSystemProvider fileSystemProvider, final TankManager tankManager, final Map<String, ?> env) throws IOException {
    this.fileSystemProvider = fileSystemProvider;
    this.tankManager = tankManager;
  }

  @Override
  public FileSystemProvider provider() {
    return this.fileSystemProvider;
  }

  @Override
  public void close() {
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public String getSeparator() {
    return "/";
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    return Collections.singleton(new TankPath(this, new byte[] {'/'}));
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    return null;
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    return null;
  }

  @Override
  public Path getPath(final String first, final String... more) {
    final String path;
    if(more.length == 0) {
      path = first;
    } else {
      final StringBuilder sb = new StringBuilder();
      sb.append(first);
      for(final String segment : more) {
        if(!segment.isEmpty()) {
          if(!sb.isEmpty()) {
            sb.append('/');
          }
          sb.append(segment);
        }
      }
      path = sb.toString();
    }
    return new TankPath(this, path.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public PathMatcher getPathMatcher(final String syntaxAndPattern) {
    final int colonIndex = syntaxAndPattern.indexOf(':');
    if(colonIndex <= 0 || colonIndex == syntaxAndPattern.length() - 1) {
      throw new IllegalArgumentException("syntaxAndPattern must have form \"syntax:pattern\" but was \"" + syntaxAndPattern + "\"");
    }

    final String syntax = syntaxAndPattern.substring(0, colonIndex);
    final String pattern = syntaxAndPattern.substring(colonIndex + 1);
    final String expr = switch(syntax) {
      case "glob" -> this.globToRegex(pattern);
      case "regex" -> pattern;
      default -> throw new UnsupportedOperationException("Unsupported syntax '" + syntax + "'");
    };
    final Pattern regex = Pattern.compile(expr);
    return path -> regex.matcher(path.toString()).matches();
  }

  private String globToRegex(final String pattern) {
    final StringBuilder sb = new StringBuilder(pattern.length());
    int inGroup = 0;
    int inClass = 0;
    int firstIndexInClass = -1;
    final char[] arr = pattern.toCharArray();
    for(int i = 0; i < arr.length; i++) {
      final char ch = arr[i];
      switch(ch) {
        case '\\':
          if(++i >= arr.length) {
            sb.append('\\');
          } else {
            final char next = arr[i];
            switch(next) {
              case ',':
                // escape not needed
                break;
              case 'Q':
              case 'E':
                // extra escape needed
                sb.append('\\');
              default:
                sb.append('\\');
            }
            sb.append(next);
          }
          break;
        case '*':
          if(inClass == 0) {
            sb.append(".*");
          } else {
            sb.append('*');
          }
          break;
        case '?':
          if(inClass == 0) {
            sb.append('.');
          } else {
            sb.append('?');
          }
          break;
        case '[':
          inClass++;
          firstIndexInClass = i + 1;
          sb.append('[');
          break;
        case ']':
          inClass--;
          sb.append(']');
          break;
        case '.':
        case '(':
        case ')':
        case '+':
        case '|':
        case '^':
        case '$':
        case '@':
        case '%':
          if(inClass == 0 || (firstIndexInClass == i && ch == '^')) {
            sb.append('\\');
          }
          sb.append(ch);
          break;
        case '!':
          if(firstIndexInClass == i) {
            sb.append('^');
          } else {
            sb.append('!');
          }
          break;
        case '{':
          inGroup++;
          sb.append('(');
          break;
        case '}':
          inGroup--;
          sb.append(')');
          break;
        case ',':
          if(inGroup > 0) {
            sb.append('|');
          } else {
            sb.append(',');
          }
          break;
        default:
          sb.append(ch);
      }
    }
    return sb.toString();
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchService newWatchService() {
    throw new UnsupportedOperationException();
  }

  public DirectoryStream<Path> newDirectoryStream(final Path dir, final DirectoryStream.Filter<? super Path> filter) throws IOException {
    return new DirectoryStream<>() {
      @Override
      public Iterator<Path> iterator() {
        return TankFileSystem.this.tankManager
          .getChildren(dir.toAbsolutePath().normalize().toString())
          .stream()
          .map(dir::resolve)
          .filter(entry -> {
            try {
              return filter.accept(entry);
            } catch(final IOException e) {
              throw new RuntimeException(e);
            }
          })
          .iterator();
      }

      @Override
      public void close() {
      }
    };
  }

  public InputStream newInputStream(final Path path, final OpenOption[] options) throws IOException {
    final String filename;

    // If just given a filename, look up the path
    if(path.toString().indexOf('/') == -1) {
      filename = this.tankManager.lookupPath(path.toString());
    } else {
      filename = path.toAbsolutePath().normalize().toString();
    }

    return new ByteArrayInputStream(this.tankManager.getFileByPath(filename));
  }

  public <A extends BasicFileAttributes> SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options, final FileAttribute<?>[] attrs) throws IOException {
    final byte[] data = this.tankManager.getFileByPath(path.toAbsolutePath().normalize().toString());

    return new SeekableByteChannel() {
      private long position;

      @Override
      public int read(final ByteBuffer dst) {
        final int l = (int)Math.min(dst.remaining(), this.size() - this.position);
        dst.put(data, (int)this.position, l);
        this.position += l;
        return l;
      }

      @Override
      public int write(final ByteBuffer src) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long position() {
        return this.position;
      }

      @Override
      public SeekableByteChannel position(final long newPosition) {
        this.position = newPosition;
        return this;
      }

      @Override
      public long size() {
        return data.length;
      }

      @Override
      public SeekableByteChannel truncate(final long size) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean isOpen() {
        return true;
      }

      @Override
      public void close() {
      }
    };
  }

  public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> clazz, final LinkOption... options) throws IOException {
    if(clazz != BasicFileAttributes.class) {
      throw new UnsupportedOperationException();
    }

    final String absolute = path.toAbsolutePath().normalize().toString();

    final String type;
    final long size;
    if(this.tankManager.isDir(absolute)) {
      type = "directory";
      size = 0;
    } else {
      type = "file";
      size = this.tankManager.getFileInfo(absolute).entrySize();
    }

    return (A)new TankFileAttributes(type, size);
  }

  private static final class TankFileAttributes implements BasicFileAttributes {
    private final String type;
    private final long size;

    private TankFileAttributes(final String type, final long size) {
      this.type = type;
      this.size = size;
    }

    @Override
    public FileTime lastModifiedTime() {
      return null;
    }

    @Override
    public FileTime lastAccessTime() {
      return null;
    }

    @Override
    public FileTime creationTime() {
      return null;
    }

    @Override
    public boolean isRegularFile() {
      return "file".equals(this.type);
    }

    @Override
    public boolean isDirectory() {
      return "directory".equals(this.type);
    }

    @Override
    public boolean isSymbolicLink() {
      return false;
    }

    @Override
    public boolean isOther() {
      return false;
    }

    @Override
    public long size() {
      return this.size;
    }

    @Override
    public Object fileKey() {
      return null;
    }
  }
}
