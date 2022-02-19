package lofimodding.opensiege.formats.tank;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TankPath implements Path {
  private final TankFileSystem fileSystem;
  private final byte[] path;
  private volatile int[] offsets;
  private volatile int hash;
  private volatile byte[] resolved;

  public TankPath(final TankFileSystem fileSystem, final byte[] path) {
    this(fileSystem, path, false);
  }

  public TankPath(final TankFileSystem fileSystem, final byte[] path, final boolean normalized) {
    this.fileSystem = fileSystem;
    if(normalized) {
      this.path = path;
    } else {
      this.path = this.normalize(path);
    }
  }

  @Override
  public TankFileSystem getFileSystem() {
    return this.fileSystem;
  }

  @Override
  public boolean isAbsolute() {
    return (this.path.length > 0) && (this.path[0] == '/');
  }

  @Override
  @Nullable
  public TankPath getRoot() {
    if(this.isAbsolute()) {
      return new TankPath(this.fileSystem, new byte[] {this.path[0]});
    }
    return null;
  }

  @Override
  @Nullable
  public TankPath getFileName() {
    this.initOffsets();
    final int nbOffsets = this.offsets.length;
    if(nbOffsets == 0) {
      return null;
    }
    if(nbOffsets == 1 && this.path[0] != '/') {
      return this;
    }
    final int offset = this.offsets[nbOffsets - 1];
    final int length = this.path.length - offset;
    final byte[] path = new byte[length];
    System.arraycopy(this.path, offset, path, 0, length);
    return new TankPath(this.fileSystem, path);
  }

  @Override
  @Nullable
  public TankPath getParent() {
    this.initOffsets();
    final int nbOffsets = this.offsets.length;
    if(nbOffsets == 0) {
      return null;
    }
    final int length = this.offsets[nbOffsets - 1] - 1;
    if(length <= 0) {
      return this.getRoot();
    }
    final byte[] path = new byte[length];
    System.arraycopy(this.path, 0, path, 0, length);
    return new TankPath(this.fileSystem, path);
  }

  @Override
  public int getNameCount() {
    this.initOffsets();
    return this.offsets.length;
  }

  @Override
  public TankPath getName(final int index) {
    this.initOffsets();
    if(index < 0 || index >= this.offsets.length) {
      throw new IllegalArgumentException();
    }
    final int offset = this.offsets[index];
    final int length;
    if(index == this.offsets.length - 1) {
      length = this.path.length - offset;
    } else {
      length = this.offsets[index + 1] - offset - 1;
    }
    final byte[] path = new byte[length];
    System.arraycopy(this.path, offset, path, 0, length);
    return new TankPath(this.fileSystem, path);

  }

  @Override
  public TankPath subpath(final int beginIndex, final int endIndex) {
    this.initOffsets();
    if((beginIndex < 0) || (beginIndex >= this.offsets.length) || (endIndex > this.offsets.length) || (beginIndex >= endIndex)) {
      throw new IllegalArgumentException();
    }
    final int offset = this.offsets[beginIndex];
    final int length;
    if(endIndex == this.offsets.length) {
      length = this.path.length - offset;
    } else {
      length = this.offsets[endIndex] - offset - 1;
    }
    final byte[] path = new byte[length];
    System.arraycopy(this.path, offset, path, 0, length);
    return new TankPath(this.fileSystem, path);
  }

  @Override
  public boolean startsWith(final Path other) {
    final TankPath p1 = this;
    final TankPath p2 = this.checkPath(other);
    if(p1.isAbsolute() != p2.isAbsolute() || p1.path.length < p2.path.length) {
      return false;
    }
    final int length = p2.path.length;
    for(int idx = 0; idx < length; idx++) {
      if(p1.path[idx] != p2.path[idx]) {
        return false;
      }
    }
    return p1.path.length == p2.path.length
      || p2.path[length - 1] == '/'
      || p1.path[length] == '/';
  }

  @Override
  public boolean startsWith(final String other) {
    return this.startsWith(this.getFileSystem().getPath(other));
  }

  @Override
  public boolean endsWith(final Path other) {
    final TankPath p1 = this;
    final TankPath p2 = this.checkPath(other);
    int i1 = p1.path.length - 1;
    if(i1 > 0 && p1.path[i1] == '/') {
      i1--;
    }
    int i2 = p2.path.length - 1;
    if(i2 > 0 && p2.path[i2] == '/') {
      i2--;
    }
    if(i2 == -1) {
      return i1 == -1;
    }
    if((p2.isAbsolute() && (!this.isAbsolute() || i2 != i1)) || (i1 < i2)) {
      return false;
    }
    for(; i2 >= 0; i1--) {
      if(p2.path[i2] != p1.path[i1]) {
        return false;
      }
      i2--;
    }
    return (p2.path[i2 + 1] == '/') || (i1 == -1) || (p1.path[i1] == '/');
  }

  @Override
  public boolean endsWith(final String other) {
    return this.endsWith(this.getFileSystem().getPath(other));
  }

  @Override
  public TankPath normalize() {
    final byte[] p = this.getResolved();
    if(p == this.path) {
      return this;
    }
    return new TankPath(this.fileSystem, p, true);
  }

  @Override
  public TankPath resolve(final Path other) {
    final TankPath p1 = this;
    final TankPath p2 = this.checkPath(other);
    if(p2.isAbsolute()) {
      return p2;
    }
    final byte[] result;
    if(p1.path[p1.path.length - 1] == '/') {
      result = new byte[p1.path.length + p2.path.length];
      System.arraycopy(p1.path, 0, result, 0, p1.path.length);
      System.arraycopy(p2.path, 0, result, p1.path.length, p2.path.length);
    } else {
      result = new byte[p1.path.length + 1 + p2.path.length];
      System.arraycopy(p1.path, 0, result, 0, p1.path.length);
      result[p1.path.length] = '/';
      System.arraycopy(p2.path, 0, result, p1.path.length + 1, p2.path.length);
    }
    return new TankPath(this.fileSystem, result);
  }

  @Override
  public TankPath resolve(final String other) {
    return this.resolve(this.getFileSystem().getPath(other));
  }

  @Override
  public Path resolveSibling(@Nullable final Path other) {
    if(other == null) {
      throw new NullPointerException();
    }
    final TankPath parent = this.getParent();
    return parent == null ? other : parent.resolve(other);
  }

  @Override
  public Path resolveSibling(final String other) {
    return this.resolveSibling(this.getFileSystem().getPath(other));
  }

  @Override
  public TankPath relativize(final Path other) {
    final TankPath p1 = this;
    final TankPath p2 = this.checkPath(other);
    if(p2.equals(p1)) {
      return new TankPath(this.fileSystem, new byte[0], true);
    }
    if(p1.isAbsolute() != p2.isAbsolute()) {
      throw new IllegalArgumentException();
    }
    // Check how many segments are common
    final int nbNames1 = p1.getNameCount();
    final int nbNames2 = p2.getNameCount();
    final int l = Math.min(nbNames1, nbNames2);
    int nbCommon = 0;
    while(nbCommon < l && equalsNameAt(p1, p2, nbCommon)) {
      nbCommon++;
    }
    int nbUp = nbNames1 - nbCommon;
    // Compute the resulting length
    int length = nbUp * 3 - 1;
    if(nbCommon < nbNames2) {
      length += p2.path.length - p2.offsets[nbCommon] + 1;
    }
    // Compute result
    final byte[] result = new byte[length];
    int idx = 0;
    while(nbUp-- > 0) {
      result[idx++] = '.';
      result[idx++] = '.';
      if(idx < length) {
        result[idx++] = '/';
      }
    }
    // Copy remaining segments
    if(nbCommon < nbNames2) {
      System.arraycopy(p2.path, p2.offsets[nbCommon], result, idx, p2.path.length - p2.offsets[nbCommon]);
    }
    return new TankPath(this.fileSystem, result);
  }

  @Override
  public URI toUri() {
    // TODO
    return null;
  }

  @Override
  public TankPath toAbsolutePath() {
    if(this.isAbsolute()) {
      return this;
    }
    final byte[] result = new byte[this.path.length + 1];
    result[0] = '/';
    System.arraycopy(this.path, 0, result, 1, this.path.length);
    return new TankPath(this.fileSystem, result, true);
  }

  @Override
  public TankPath toRealPath(final LinkOption... options) throws IOException {
    final TankPath absolute = new TankPath(this.fileSystem, this.getResolvedPath()).toAbsolutePath();
    this.fileSystem.provider().checkAccess(absolute);
    return absolute;
  }

  @Override
  public File toFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>[] events, final WatchEvent.Modifier... modifiers) {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(final WatchService watcher, final WatchEvent.Kind<?>... events) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<Path> iterator() {
    return new Iterator<>() {
      private int index;

      @Override
      public boolean hasNext() {
        return this.index < TankPath.this.getNameCount();
      }

      @Override
      public Path next() {
        if(this.index < TankPath.this.getNameCount()) {
          final TankPath name = TankPath.this.getName(this.index);
          this.index++;
          return name;
        }
        throw new NoSuchElementException();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public int compareTo(final Path paramPath) {
    final TankPath p1 = this;
    final TankPath p2 = this.checkPath(paramPath);
    final byte[] a1 = p1.path;
    final byte[] a2 = p2.path;
    final int l1 = a1.length;
    final int l2 = a2.length;
    for(int i = 0, l = Math.min(l1, l2); i < l; i++) {
      final int b1 = a1[i] & 0xFF;
      final int b2 = a2[i] & 0xFF;
      if(b1 != b2) {
        return b1 - b2;
      }
    }
    return l1 - l2;
  }

  private TankPath checkPath(@Nullable final Path paramPath) {
    if(paramPath == null) {
      throw new NullPointerException();
    }
    if(!(paramPath instanceof TankPath)) {
      throw new ProviderMismatchException();
    }
    return (TankPath)paramPath;
  }

  @Override
  public int hashCode() {
    if(this.hash == 0) {
      this.hash = Arrays.hashCode(this.path);
    }

    return this.hash;
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof TankPath
      && ((TankPath)obj).fileSystem == this.fileSystem
      && this.compareTo((TankPath)obj) == 0;
  }

  @Override
  public String toString() {
    return new String(this.path, StandardCharsets.UTF_8);
  }

  private void initOffsets() {
    if(this.offsets == null) {
      int count = 0;
      int index = 0;
      while(index < this.path.length) {
        final byte c = this.path[index++];
        if(c != '/') {
          count++;
          while(index < this.path.length && this.path[index] != '/') {
            index++;
          }
        }
      }
      final int[] result = new int[count];
      count = 0;
      index = 0;
      while(index < this.path.length) {
        final int m = this.path[index];
        if(m == '/') {
          index++;
        } else {
          result[count++] = index++;
          while(index < this.path.length && this.path[index] != '/') {
            index++;
          }
        }
      }
      synchronized(this) {
        if(this.offsets == null) {
          this.offsets = result;
        }
      }
    }
  }

  byte[] getResolvedPath() {
    if(this.resolved == null) {
      this.resolved = this.isAbsolute() ? this.getResolved() : this.toAbsolutePath().getResolvedPath();
    }

    return this.resolved;
  }


  private byte[] normalize(final byte[] path) {
    if(path.length == 0) {
      return path;
    }
    int i = 0;
    for(int j = 0; j < path.length; j++) {
      final int k = path[j];
      if(k == '\\') {
        return this.normalize(path, j);
      }
      if((k == '/') && (i == '/')) {
        return this.normalize(path, j - 1);
      }
      if(k == 0) {
        throw new InvalidPathException(new String(path, StandardCharsets.UTF_8), "Path: nul character not allowed");
      }
      i = k;
    }
    return path;
  }

  private byte[] normalize(final byte[] path, final int index) {
    final byte[] arrayOfByte = new byte[path.length];
    int i = 0;
    while(i < index) {
      arrayOfByte[i] = path[i];
      i++;
    }
    int j = i;
    int k = 0;
    while(i < path.length) {
      int m = path[i++];
      if(m == '\\') {
        m = '/';
      }
      if((m != '/') || (k != '/')) {
        if(m == 0) {
          throw new InvalidPathException(new String(path, StandardCharsets.UTF_8), "Path: nul character not allowed");
        }
        arrayOfByte[j++] = (byte)m;
        k = m;
      }
    }
    if((j > 1) && (arrayOfByte[j - 1] == '/')) {
      j--;
    }
    return j == arrayOfByte.length ? arrayOfByte : Arrays.copyOf(arrayOfByte, j);
  }

  private byte[] getResolved() {
    if(this.path.length == 0) {
      return this.path;
    }
    for(final byte c : this.path) {
      if(c == '.') {
        return doGetResolved(this);
      }
    }
    return this.path;
  }

  private static byte[] doGetResolved(final TankPath p) {
    final int nc = p.getNameCount();
    final byte[] path = p.path;
    final int[] offsets = p.offsets;
    final byte[] to = new byte[path.length];
    final int[] lastM = new int[nc];
    int lastMOff = -1;
    int m = 0;
    for(int i = 0; i < nc; i++) {
      int n = offsets[i];
      int len = (i == offsets.length - 1) ? (path.length - n) : (offsets[i + 1] - n - 1);
      if(len == 1 && path[n] == (byte)'.') {
        if(m == 0 && path[0] == '/')   // absolute path
        {
          to[m++] = '/';
        }
        continue;
      }
      if(len == 2 && path[n] == '.' && path[n + 1] == '.') {
        if(lastMOff >= 0) {
          m = lastM[lastMOff--];  // retreat
          continue;
        }
        if(path[0] == '/') {  // "/../xyz" skip
          if(m == 0) {
            to[m++] = '/';
          }
        } else {               // "../xyz" -> "../xyz"
          if(m != 0 && to[m - 1] != '/') {
            to[m++] = '/';
          }
          while(len-- > 0) {
            to[m++] = path[n++];
          }
        }
        continue;
      }
      if(m == 0 && path[0] == '/' ||   // absolute path
        m != 0 && to[m - 1] != '/') {   // not the first name
        to[m++] = '/';
      }
      lastM[++lastMOff] = m;
      while(len-- > 0) {
        to[m++] = path[n++];
      }
    }
    if(m > 1 && to[m - 1] == '/') {
      m--;
    }
    return (m == to.length) ? to : Arrays.copyOf(to, m);
  }

  private static boolean equalsNameAt(final TankPath p1, final TankPath p2, final int index) {
    final int beg1 = p1.offsets[index];
    final int len1;
    if(index == p1.offsets.length - 1) {
      len1 = p1.path.length - beg1;
    } else {
      len1 = p1.offsets[index + 1] - beg1 - 1;
    }
    final int beg2 = p2.offsets[index];
    final int len2;
    if(index == p2.offsets.length - 1) {
      len2 = p2.path.length - beg2;
    } else {
      len2 = p2.offsets[index + 1] - beg2 - 1;
    }
    if(len1 != len2) {
      return false;
    }
    for(int n = 0; n < len1; n++) {
      if(p1.path[beg1 + n] != p2.path[beg2 + n]) {
        return false;
      }
    }
    return true;
  }
}
