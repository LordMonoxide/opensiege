package lofimodding.opensiege.formats.skrit.exceptions;

public class InvalidVariableInitializerException extends SkritCompilerException {
  public InvalidVariableInitializerException(final String message) {
    super(message);
  }

  public InvalidVariableInitializerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
