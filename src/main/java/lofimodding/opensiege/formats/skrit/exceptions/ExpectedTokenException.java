package lofimodding.opensiege.formats.skrit.exceptions;

public class ExpectedTokenException extends SkritCompilerException {
  public ExpectedTokenException(final String message) {
    super(message);
  }

  public ExpectedTokenException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
