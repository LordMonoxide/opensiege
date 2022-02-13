package lofimodding.opensiege.formats.gas;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import lofimodding.opensiege.world.WorldPos;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

public final class GasLoader {
  private GasLoader() { }

  private static final Pattern WORLD_POS_REGEX = Pattern.compile("\\d+\\.\\d+\\s*,\\s*\\d+\\.\\d+\\s*,\\s*\\d+\\.\\d+\\s*,\\s*0x[\\da-z]+\\s*");
  private static final Pattern TIME_REGEX = Pattern.compile("/\\d+h\\d+m/");

  private static final Reader READ_HEADER = new ReadHeader();
  private static final Reader READ_BRACE = new ReadChar('{') {
    @Override
    public boolean read(final StateManager stateManager) {
      final boolean val = super.read(stateManager);

      if(val) {
        stateManager.push();
      }

      return val;
    }
  };
  private static final Reader READ_CLOSE = new ReadChar('}') {
    @Override
    public boolean read(final StateManager stateManager) {
      final boolean val = super.read(stateManager);

      if(val) {
        stateManager.pop();

        if(stateManager.current.size() == 1) {
          stateManager.stop();
        } else {
          stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
        }
      }

      return val;
    }
  };
  private static final Reader READ_EQUAL = new ReadChar('=');
  private static final Reader READ_SEMICOLON = new ReadChar(';');
  private static final Reader READ_KEY = new ReadKey();
  private static final Reader READ_VALUE = new ReadValue();
  private static final Reader READ_BOOL_VALUE = new ReadBoolValue();
  private static final Reader READ_FLOAT_VALUE = new ReadFloatValue();
  private static final Reader READ_HEX_VALUE = new ReadHexValue();

  private static final State READ_OBJECT_STATE = new State(READ_HEADER, READ_BRACE);
  private static final State READ_PROPERTY_STATE = new State(READ_KEY, READ_EQUAL);
  private static final State READ_CLOSE_STATE = new State(READ_CLOSE);
  private static final State READ_PROPERTY_OR_HEADER_STATE = new State(or(READ_PROPERTY_STATE, READ_OBJECT_STATE, READ_CLOSE_STATE));
  private static final State READ_VALUE_STATE = new State(READ_VALUE, READ_SEMICOLON);
  private static final State READ_BOOL_VALUE_STATE = new State(READ_BOOL_VALUE, READ_SEMICOLON);
  private static final State READ_FLOAT_VALUE_STATE = new State(READ_FLOAT_VALUE, READ_SEMICOLON);
  private static final State READ_HEX_VALUE_STATE = new State(READ_HEX_VALUE, READ_SEMICOLON);

  public static void load(final InputStream file) {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(file));

    final StringBuilder builder = new StringBuilder();
    reader.lines().forEach(line -> builder.append(line).append('\n'));

    final String gas = builder.toString();

    final StateManager stateManager = new StateManager(gas);

    outer:
    while(stateManager.hasMore()) {
      for(final Reader r : stateManager.state.readers) {
        stateManager.skipWhitespace();

        if(!r.read(stateManager)) {
          System.err.println("Failed to load gas " + stateManager.index);
          return;
        }

        if(stateManager.stop) {
          break outer;
        }
      }

      if(stateManager.newState != null) {
        stateManager.state = stateManager.newState;
        stateManager.newState = null;
      } else {
        System.err.println("Failed to load gas " + stateManager.index); //TODO explain why
        break;
      }
    }
  }

  private static Reader or(final State... states) {
    return stateManager -> {
      outer:
      for(final State state : states) {
        for(final Reader reader : state.readers) {
          stateManager.skipWhitespace();

          if(!reader.read(stateManager)) {
            continue outer;
          }
        }

        return true;
      }

      return false;
    };
  }

  private static final class StateManager {
    private final String gas;

    private State state = READ_OBJECT_STATE;
    @Nullable
    private State newState;
    private int index;
    private int mark;

    private boolean stop;

    private String header;
    private final Map<String, Object> properties = new HashMap<>();
    private final Deque<Map<String, Object>> current = new LinkedList<>();
    @Nullable
    private String key;

    private StateManager(final String gas) {
      this.gas = gas;
      this.current.push(this.properties);
    }

    public void setHeader(final String header) {
      this.header = header;
    }

    public void setKey(final String key) {
      this.key = key;
    }

    public void setValue(final Object value) {
      if(this.key == null) {
        throw new IllegalStateException("Key must be set before value");
      }

      this.current.element().put(this.key, value);
      this.key = null;
    }

    public boolean hasMore() {
      return this.index < this.gas.length();
    }

    public void changeState(final State state) {
      this.newState = state;
    }

    public void advance(final int amount) {
      this.index += amount;
    }

    public void advance() {
      this.advance(1);
    }

    public char read() {
      return this.gas.charAt(this.index);
    }

    public String read(final int length) {
      return this.gas.substring(this.index, this.index + length);
    }

    public String readUntil(final char c) {
      final int oldMark = this.mark;
      int at = -1;

      this.mark();

      while(this.hasMore()) {
        if(this.read() == c) {
          at = this.index;
          break;
        }

        this.advance();
      }

      this.reset();
      this.mark = oldMark;

      if(at == -1) {
        return "";
      }

      return this.read(at - this.index);
    }

    public String readKey() {
      final int oldMark = this.mark;
      int at = -1;

      this.mark();

      while(this.hasMore()) {
        final char c = this.read();

        if((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && c != '_' && (c < '0' || c > '9')) {
          at = this.index;
          break;
        }

        this.advance();
      }

      this.reset();
      this.mark = oldMark;

      if(at == -1) {
        return "";
      }

      return this.read(at - this.index);
    }

    public String readValue() {
      final int oldMark = this.mark;
      int at = -1;

      this.mark();

      while(this.hasMore()) {
        final char c = this.read();

        if((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_' && c != '.') {
          at = this.index;
          break;
        }

        this.advance();
      }

      this.reset();
      this.mark = oldMark;

      if(at == -1) {
        return "";
      }

      return this.read(at - this.index);
    }

    public void skipWhitespace() {
      while(true) {
        final char c = this.read();

        if(c != ' ' && c != '\t' && c != '\n' && c != '\r') {
          break;
        }

        this.advance();
      }
    }

    public void mark() {
      this.mark = this.index;
    }

    public void reset() {
      this.index = this.mark;
    }

    public void push() {
      if(this.current.element().containsKey(this.header)) {
        throw new IllegalStateException("Key " + this.header + " already defined");
      }

      final Map<String, Object> map = new HashMap<>();
      this.current.element().put(this.header, map);
      this.current.push(map);
    }

    public void pop() {
      this.current.pop();
    }

    public void stop() {
      this.stop = true;
    }
  }

  private interface Reader {
    boolean read(final StateManager stateManager);
  }

  private static class ReadHeader implements Reader {
    @Override
    public boolean read(final StateManager stateManager) {
      if(stateManager.read() == '[') {
        stateManager.advance();
        final String header = stateManager.readUntil(']');

        if(!header.isEmpty()) {
          stateManager.setHeader(header);
          stateManager.advance(header.length() + 1);
          stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
          return true;
        }
      }

      return false;
    }
  }

  private static class ReadChar implements Reader {
    private final char c;

    public ReadChar(final char c) {
      this.c = c;
    }

    @Override
    public boolean read(final StateManager stateManager) {
      if(stateManager.read() == this.c) {
        stateManager.advance();
        return true;
      }

      return false;
    }
  }

  private static class ReadKey implements Reader {
    @Override
    public boolean read(final StateManager stateManager) {
      final String type = stateManager.readKey();
      stateManager.advance(type.length());
      stateManager.skipWhitespace();

      final String key;
      final State newState;

      switch(type) {
        case "b" -> {
          key = stateManager.readKey();
          newState = READ_BOOL_VALUE_STATE;
          stateManager.advance(key.length());
        }

        case "f" -> {
          key = stateManager.readKey();
          newState = READ_FLOAT_VALUE_STATE;
          stateManager.advance(key.length());
        }

        case "x" -> {
          key = stateManager.readKey();
          newState = READ_HEX_VALUE_STATE;
          stateManager.advance(key.length());
        }

        default -> {
          key = type;
          newState = READ_VALUE_STATE;
        }
      }

      if(key.isEmpty()) {
        return false;
      }

      stateManager.setKey(key);
      stateManager.changeState(newState);
      return true;
    }
  }

  private static class ReadValue implements Reader {
    @Override
    public boolean read(final StateManager stateManager) {
      Object val;
      FloatList floats = null;

      if(stateManager.read() == '"') {
        stateManager.advance();
        final String str = stateManager.readUntil('"');
        stateManager.advance(str.length() + 1);
        val = str;
      } else {
        while(true) {
          final String wp = stateManager.readUntil(';');

          if(WORLD_POS_REGEX.matcher(wp).matches()) {
            final Vector3f pos = new Vector3f();
            for(int i = 0; i < 3; i++) {
              final String s = stateManager.readValue();
              stateManager.advance(s.length());

              pos.setComponent(i, Float.parseFloat(s));

              stateManager.skipWhitespace();
              if(stateManager.read() != ',') {
                System.err.println("Invalid WorldPos " + wp);
                return false;
              }
              stateManager.advance();
              stateManager.skipWhitespace();
            }

            final String s = stateManager.readValue();
            stateManager.advance(s.length());

            if(s.startsWith("0x")) {
              try {
                val = new WorldPos(Long.parseLong(s.substring(2), 16), pos.x, pos.y, pos.z);
                break;
              } catch(final NumberFormatException ignored) {}

              System.err.println("Invalid hex " + wp);
              return false;
            }

            System.err.println("Missing node ID " + wp);
            return false;
          }

          final String str = stateManager.readValue();
          stateManager.advance(str.length());

          try {
            val = Integer.parseInt(str);
            break;
          } catch(final NumberFormatException ignored) { }

          if(str.startsWith("0x")) {
            try {
              val = Integer.parseInt(str.substring(2), 16);
              break;
            } catch(final NumberFormatException ignored) {}

            System.err.println("Invalid hex " + str);
            return false;
          }

          try {
            final float f = Float.parseFloat(str);

            stateManager.skipWhitespace();

            if(stateManager.read() != ',') {
              val = f;
              break;
            }

            // It's a world position (x, y, z, snode)
            if(floats == null) {
              floats = new FloatArrayList();
            }

            floats.add(f);
            stateManager.advance();
            stateManager.skipWhitespace();
            continue;
          } catch(final NumberFormatException ignored) { }

          if(floats != null) {
            System.err.println("Non-float in vec");
            return false;
          }

          if("true".equals(str) || "false".equals(str)) {
            val = Boolean.parseBoolean(str);
            break;
          }

          if(TIME_REGEX.matcher(str).matches()) {
            val = str; //TODO
            break;
          }

          if(!str.isEmpty()) {
            val = str; //TODO identifier
            break;
          }

          return false;
        }
      }

      if(floats != null) {
        if(floats.size() == 2) {
          stateManager.setValue(new Vector2f(floats.getFloat(0), floats.getFloat(1)));
        } else if(floats.size() == 3) {
          stateManager.setValue(new Vector3f(floats.getFloat(0), floats.getFloat(1), floats.getFloat(2)));
        } else if(floats.size() == 4) {
          stateManager.setValue(new Vector4f(floats.getFloat(0), floats.getFloat(1), floats.getFloat(2), floats.getFloat(2)));
        } else {
          System.err.println("Wrong number of floats in vec - " + floats.size());
          return false;
        }
      } else {
        stateManager.setValue(val);
      }

      stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
      return true;
    }
  }

  private static class ReadBoolValue implements Reader {
    @Override
    public boolean read(final StateManager stateManager) {
      final String value = stateManager.readValue();
      stateManager.advance(value.length());

      if("true".equals(value)) {
        stateManager.setValue(true);
      } else if("false".equals(value)) {
        stateManager.setValue(false);
      } else {
        System.err.println("Invalid boolean value " + value);
        return false;
      }

      stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
      return true;
    }
  }

  private static class ReadFloatValue implements Reader {
    @Override
    public boolean read(final StateManager stateManager) {
      final String value = stateManager.readValue();
      stateManager.advance(value.length());

      final float val;
      try {
        val = Float.parseFloat(value);
      } catch(final NumberFormatException e) {
        System.err.println("Invalid float value " + value);
        return false;
      }

      stateManager.setValue(val);
      stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
      return true;
    }
  }

  private static class ReadHexValue implements Reader {
    @Override
    public boolean read(final StateManager stateManager) {
      final String value = stateManager.readValue();
      stateManager.advance(value.length());

      final int val;
      try {
        val = Integer.parseInt(value, 16);
      } catch(final NumberFormatException e) {
        System.err.println("Invalid hex value " + value);
        return false;
      }

      stateManager.setValue(val);
      stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
      return true;
    }
  }

  private static class State {
    private final Reader[] readers;

    public State(final Reader... readers) {
      this.readers = readers;
    }
  }
}
