package lofimodding.opensiege.formats.skrit.exceptions;

public class SkritCompilerException extends RuntimeException {
  public SkritCompilerException(final String message) {
    super(message);
  }

  public SkritCompilerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
