package lofimodding.opensiege.formats.gas;

public class GasParserException extends Exception {
  public GasParserException() {
    super();
  }

  public GasParserException(final String message) {
    super(message);
  }

  public GasParserException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public GasParserException(final Throwable cause) {
    super(cause);
  }

  protected GasParserException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
