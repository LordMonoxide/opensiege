package lofimodding.opensiege.go;

public record GoId(String value) {
  @Override
  public String toString() {
    return this.value;
  }
}
