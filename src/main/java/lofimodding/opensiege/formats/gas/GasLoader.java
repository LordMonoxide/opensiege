package lofimodding.opensiege.formats.gas;

import lofimodding.opensiege.world.WorldPos;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GasLoader {
  private GasLoader() { }

  private static final Pattern WORLD_POS_REGEX = Pattern.compile("\\d+\\.\\d+\\s*,\\s*\\d+\\.\\d+\\s*,\\s*\\d+\\.\\d+\\s*,\\s*0x[\\da-z]+\\s*");
  private static final Pattern TIME_REGEX = Pattern.compile("(\\d+)h(\\d+)m");

  private static final Reader READ_HEADER = new ReadHeader();
  private static final Reader READ_BRACE = new ReadChar('{') {
    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      super.read(stateManager);
      stateManager.push();
    }
  };
  private static final Reader READ_CLOSE = new ReadChar('}') {
    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      super.read(stateManager);
      stateManager.pop();

      if(stateManager.current.size() == 1) {
        stateManager.stop();
      } else {
        stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
      }
    }
  };
  private static final Reader READ_EQUAL = new ReadChar('=');
  private static final Reader READ_SEMICOLON = new ReadChar(';');
  private static final Reader READ_KEY = new ReadKey();
  private static final Reader READ_VALUE = new ReadValue();
  private static final Reader READ_BOOL_VALUE = new ReadBoolValue();
  private static final Reader READ_FLOAT_VALUE = new ReadFloatValue();
  private static final Reader READ_HEX_VALUE = new ReadHexValue();
  private static final Reader READ_INT_VALUE = new ReadIntValue();

  private static final State READ_OBJECT_STATE = new State(READ_HEADER, READ_BRACE);
  private static final State READ_PROPERTY_STATE = new State(READ_KEY, READ_EQUAL);
  private static final State READ_CLOSE_STATE = new State(READ_CLOSE);
  private static final State READ_PROPERTY_OR_HEADER_STATE = new State(or(READ_PROPERTY_STATE, READ_OBJECT_STATE, READ_CLOSE_STATE));
  private static final State READ_VALUE_STATE = new State(READ_VALUE, READ_SEMICOLON);
  private static final State READ_BOOL_VALUE_STATE = new State(READ_BOOL_VALUE, READ_SEMICOLON);
  private static final State READ_FLOAT_VALUE_STATE = new State(READ_FLOAT_VALUE, READ_SEMICOLON);
  private static final State READ_HEX_VALUE_STATE = new State(READ_HEX_VALUE, READ_SEMICOLON);
  private static final State READ_INT_VALUE_STATE = new State(READ_INT_VALUE, READ_SEMICOLON);

  public static GasEntry load(final InputStream file) {
    final BufferedReader reader = new BufferedReader(new InputStreamReader(file));

    final StringBuilder builder = new StringBuilder();
    final List<String> lines = new ArrayList<>();
    reader.lines().forEach(line -> {
      lines.add(line);
      builder.append(line).append('\n');
    });

    final String gas = builder.toString();

    final StateManager stateManager = new StateManager(gas);

    outer:
    while(stateManager.hasMore()) {
      for(final Reader r : stateManager.state.readers) {
        stateManager.skipWhitespaceAndComments();

        final int index = stateManager.index;

        try {
          r.read(stateManager);
        } catch(final GasParserException e) {
          int charsLeft = index;
          int lineNumber;
          for(lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            final String line = lines.get(lineNumber);
            if(charsLeft < line.length()) {
              break;
            }

            charsLeft -= line.length();
          }

          System.err.println("Failed to load gas - line " + (lineNumber - 1) + " char " + charsLeft + ": " + e.getMessage() + ", got " + stateManager.readUntil(c -> c == '\r' || c == '\n'));
          e.printStackTrace();
          return new GasEntry(new HashMap<>(), new HashMap<>(), new HashMap<>());
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

    return buildEntry(stateManager.properties);
  }

  private static GasEntry buildEntry(final Map<String, Object> map) {
    final Map<String, GasEntry> children = new HashMap<>();
    final Map<String, List<GasEntry>> arrayChildren = new HashMap<>();
    final Map<String, Object> values = new HashMap<>();

    for(final Map.Entry<String, Object> entry : map.entrySet()) {
      if(entry.getValue() instanceof final List child) {
        for(final Object o : child) {
          final Map<String, Object> childMap = (Map<String, Object>)o;
          arrayChildren.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(buildEntry(childMap));
        }
      } else if(entry.getValue() instanceof final Map child) {
        children.put(entry.getKey(), buildEntry(child));
      } else {
        values.put(entry.getKey(), entry.getValue());
      }
    }

    return new GasEntry(children, arrayChildren, values);
  }

  private static Reader or(final State... states) {
    return stateManager -> {
      String[] errors = null;

      outer:
      for(final State state : states) {
        for(int i = 0; i < state.readers.length; i++) {
          final Reader reader = state.readers[i];
          stateManager.skipWhitespaceAndComments();

          try {
            reader.read(stateManager);
          } catch(final GasParserException e) {
            if(errors == null) {
              errors = new String[states.length];
            }

            errors[i] = e.getMessage();
            continue outer;
          }
        }

        return;
      }

      final String error;
      if(errors != null) {
        error = "Expected one of the following: " + String.join("; ", errors);
      } else {
        error = "An unknown error occurred.";
      }

      throw new GasParserException(error);
    };
  }

  private interface CharPredicate {
    boolean test(final char c);
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
    private boolean headerList;
    private final Map<String, Object> properties = new HashMap<>();
    private final Deque<Map<String, Object>> current = new LinkedList<>();
    @Nullable
    private String key;
    private boolean keyList;

    private StateManager(final String gas) {
      this.gas = gas;
      this.current.push(this.properties);
    }

    public void setHeader(final String header) {
      if(header.endsWith("*")) {
        this.header = header.substring(0, header.length() - 1);
        this.headerList = true;
      } else {
        this.header = header;
        this.headerList = false;
      }
    }

    public void setKey(final String key) {
      if(key.endsWith("*")) {
        this.key = key.substring(0, key.length() - 1);
        this.keyList = true;
      } else {
        this.key = key;
        this.keyList = false;
      }
    }

    public void setValue(final Object value) throws GasParserException {
      if(this.key == null) {
        throw new IllegalStateException("Key must be set before value");
      }

      if(this.keyList) {
        final Object obj = this.current.element().computeIfAbsent(this.key, key -> new ArrayList<>());

        if(obj instanceof final List list) {
          list.add(value);
        } else {
          throw new GasParserException("List value and non-list value have the same key: " + this.key);
        }
      } else {
        this.current.element().put(this.key, value);
      }

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

    public String readUntil(final CharPredicate until) {
      final int oldMark = this.mark;
      int at = -1;

      this.mark();

      while(this.hasMore()) {
        if(until.test(this.read())) {
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

    public String readUntil(final String until) {
      final int oldMark = this.mark;
      int at = -1;

      this.mark();

      while(this.hasMore()) {
        this.skipUntil(c -> c == until.charAt(0));

        if(this.read(until.length()).equals(until)) {
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

    public String readUntil(final char c) {
      return this.readUntil(c1 -> c1 == c);
    }

    public String readKey() {
      return this.readUntil(c -> (c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_');
    }

    public String readValue() {
      return this.readUntil(c -> (c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '_' && c != '.' && c != '\\');
    }

    public void skipUntil(final CharPredicate until) {
      while(this.hasMore()) {
        if(until.test(this.read())) {
          break;
        }

        this.advance();
      }
    }

    public void skipUntil(final String until) {
      final int mark = this.mark;
      this.mark();

      while(this.hasMore()) {
        this.skipUntil(c -> c == until.charAt(0));

        if(this.read(until.length()).equals(until)) {
          this.advance(until.length());
          return;
        }

        this.advance();
      }

      this.reset();
      this.mark = mark;
    }

    public void skipWhitespaceAndComments() {
      int currentIndex;

      // Repeat operation until we no longer find whitespace or comments
      do {
        currentIndex = this.index;

        // Skip whitespace
        this.skipUntil(c -> c != ' ' && c != '\t' && c != '\n' && c != '\r');

        // Skip line comments
        if("//".equals(this.read(2))) {
          this.skipUntil(c -> c == '\n' || c == '\r');
        }

        // Skip block comments
        if("/*".equals(this.read(2))) {
          this.skipUntil("*/");
        }
      } while(currentIndex != this.index);
    }

    public void mark() {
      this.mark = this.index;
    }

    public void reset() {
      this.index = this.mark;
    }

    public void push() throws GasParserException {
      if(this.current.element().containsKey(this.header) && !this.headerList) {
        throw new GasParserException("Key " + this.header + " already defined");
      }

      final Map<String, Object> map = new HashMap<>();

      if(this.headerList && this.current.size() > 1) {
        final List<Object> list = (List<Object>)this.current.element().computeIfAbsent(this.header, key -> new ArrayList<>());
        list.add(map);
      } else {
        this.current.element().put(this.header, map);
      }

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
    void read(final StateManager stateManager) throws GasParserException;
  }

  private static class ReadHeader implements Reader {
    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      if(stateManager.read() == '[') {
        stateManager.advance();
        final String header = stateManager.readUntil(']');

        if(!header.isEmpty()) {
          stateManager.setHeader(header);
          stateManager.advance(header.length() + 1);
          stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
          return;
        }
      }

      throw new GasParserException("Expected header");
    }
  }

  private static class ReadChar implements Reader {
    private final char c;

    public ReadChar(final char c) {
      this.c = c;
    }

    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      if(stateManager.read() == this.c) {
        stateManager.advance();
        return;
      }

      throw new GasParserException("Expected " + this.c);
    }
  }

  private static class ReadKey implements Reader {
    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      final String type = stateManager.readKey();
      stateManager.advance(type.length());
      stateManager.skipWhitespaceAndComments();

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

        case "i" -> {
          key = stateManager.readKey();
          newState = READ_INT_VALUE_STATE;
          stateManager.advance(key.length());
        }

        default -> {
          key = type;
          newState = READ_VALUE_STATE;
        }
      }

      if(key.isEmpty()) {
        throw new GasParserException("Expected key");
      }

      stateManager.setKey(key);
      stateManager.changeState(newState);
    }
  }

  private static class ReadValue implements Reader {
    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      @Nullable
      Object val;

      if(stateManager.read() == '"') {
        stateManager.advance();
        final String str = stateManager.readUntil('"');
        stateManager.advance(str.length() + 1);
        val = str;
      } else if(stateManager.read() == '<') {
        stateManager.advance();
        final String str = stateManager.readUntil('>');
        stateManager.advance(str.length() + 1);
        val = new GasBracketType(str);
      } else {
        while(true) {
          final String str = stateManager.readUntil(';').trim();

          if(WORLD_POS_REGEX.matcher(str).matches()) {
            final Vector3f pos = new Vector3f();
            for(int i = 0; i < 3; i++) {
              final String s = stateManager.readValue();
              stateManager.advance(s.length());

              pos.setComponent(i, Float.parseFloat(s));

              stateManager.skipWhitespaceAndComments();
              if(stateManager.read() != ',') {
                throw new GasParserException("Invalid WorldPos");
              }
              stateManager.advance();
              stateManager.skipWhitespaceAndComments();
            }

            final String s = stateManager.readValue();
            stateManager.advance(s.length());

            if(s.startsWith("0x")) {
              try {
                val = new WorldPos(Integer.parseUnsignedInt(s.substring(2), 16), pos.x, pos.y, pos.z);
                break;
              } catch(final NumberFormatException ignored) {}

              throw new GasParserException("Invalid hex");
            }

            throw new GasParserException("Missing node ID");
          }

          stateManager.advance(str.length());

          if("none".equals(str)) {
            val = null;
            break;
          }

          try {
            val = Integer.parseInt(str);
            break;
          } catch(final NumberFormatException ignored) { }

          if(str.startsWith("0x")) {
            try {
              val = Integer.parseUnsignedInt(str.substring(2), 16);
              break;
            } catch(final NumberFormatException ignored) {}

            throw new GasParserException("Invalid hex");
          }

          try {
            val = Float.parseFloat(str);
            break;
          } catch(final NumberFormatException ignored) { }

          if("true".equals(str) || "false".equals(str)) {
            val = Boolean.parseBoolean(str);
            break;
          }

          final Matcher timeMatcher = TIME_REGEX.matcher(str);
          if(timeMatcher.matches()) {
            final int hours = Integer.parseInt(timeMatcher.group(1));
            final int minutes = Integer.parseInt(timeMatcher.group(2));

            val = LocalTime.of(hours, minutes);
            break;
          }

          val = str;
          break;
        }
      }

      stateManager.setValue(val);
      stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
    }
  }

  private static class ReadBoolValue implements Reader {
    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      final String value = stateManager.readValue();
      stateManager.advance(value.length());

      if("true".equals(value)) {
        stateManager.setValue(true);
      } else if("false".equals(value)) {
        stateManager.setValue(false);
      } else {
        throw new GasParserException("Expected boolean value");
      }

      stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
    }
  }

  private static class ReadFloatValue implements Reader {
    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      final String value = stateManager.readValue();
      stateManager.advance(value.length());

      final float val;
      try {
        val = Float.parseFloat(value);
      } catch(final NumberFormatException e) {
        throw new GasParserException("Expected float value");
      }

      stateManager.setValue(val);
      stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
    }
  }

  private static class ReadHexValue implements Reader {
    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      final String value = stateManager.readValue();
      stateManager.advance(value.length());

      final int val;
      try {
        val = Integer.parseUnsignedInt(value.substring(2), 16);
      } catch(final NumberFormatException e) {
        throw new GasParserException("Expected hex value");
      }

      stateManager.setValue(val);
      stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
    }
  }

  private static class ReadIntValue implements Reader {
    @Override
    public void read(final StateManager stateManager) throws GasParserException {
      final String value = stateManager.readValue();
      stateManager.advance(value.length());

      final int val;
      try {
        val = Integer.parseInt(value);
      } catch(final NumberFormatException e) {
        throw new GasParserException("Expected int value");
      }

      stateManager.setValue(val);
      stateManager.changeState(READ_PROPERTY_OR_HEADER_STATE);
    }
  }

  private static class State {
    private final Reader[] readers;

    public State(final Reader... readers) {
      this.readers = readers;
    }
  }
}
