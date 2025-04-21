package lofimodding.opensiege.formats.skrit.exceptions;

public class UnexpectedTokenException extends SkritCompilerException {
  public UnexpectedTokenException(final String message) {
    super(message);
  }

  public UnexpectedTokenException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
