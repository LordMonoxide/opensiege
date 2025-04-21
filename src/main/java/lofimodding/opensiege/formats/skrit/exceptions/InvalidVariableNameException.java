package lofimodding.opensiege.formats.skrit.exceptions;

public class InvalidVariableNameException extends SkritCompilerException {
  public InvalidVariableNameException(final String message) {
    super(message);
  }

  public InvalidVariableNameException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
