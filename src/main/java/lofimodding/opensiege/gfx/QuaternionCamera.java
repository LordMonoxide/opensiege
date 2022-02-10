package lofimodding.opensiege.gfx;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.nio.FloatBuffer;

public class QuaternionCamera implements Camera {
  private static final float PiD2 = (float)(Math.PI / 2.0d);
  private static final Vector3f UP = new Vector3f(0.0f, 1.0f, 0.0f);

  private final Matrix4f view = new Matrix4f();

  private final Vector3f pos = new Vector3f();
  private final Vector3f front = new Vector3f();
  private final Vector3f right = new Vector3f();
  private final Vector3f center = new Vector3f();
  private final Quaternionf yaw = new Quaternionf();
  private final Quaternionf pitch = new Quaternionf();

  public QuaternionCamera(final float x, final float y, final float z) {
    this.pos.set(x, y, z);
    this.look(0.0f, 0.0f);
  }

  @Override
  public float getX() {
    return this.pos.x;
  }

  @Override
  public float getY() {
    return this.pos.y;
  }

  @Override
  public float getZ() {
    return this.pos.z;
  }

  @Override
  public void move(final float amount) {
    this.front.mul(amount, this.center);
    this.pos.add(this.center);
    this.update();
  }

  @Override
  public void strafe(final float amount) {
    this.right.mul(amount, this.center);
    this.pos.add(this.center);
    this.update();
  }

  @Override
  public void jump(final float amount) {
    UP.mul(amount, this.center);
    this.pos.add(this.center);
    this.update();
  }

  @Override
  public void look(final float yaw, final float pitch) {
    //TODO: not really sure why this is necessary.  Without this, movement is rotated by Pi/2 radians (i.e. right is backwards).
    final float adjustedYaw = yaw - PiD2;

    this.front.set((float)Math.cos(adjustedYaw), 0.0f, (float)Math.sin(adjustedYaw)).normalize();
    this.front.cross(UP, this.right).normalize();

    this.yaw.fromAxisAngleRad(0.0f, 1.0f, 0.0f, yaw);
    this.pitch.fromAxisAngleRad((float)Math.cos(yaw), 0.0f, (float)Math.sin(yaw), pitch);
    this.yaw.mul(this.pitch);
    this.update();
  }

  private void update() {
    this.pos.negate(this.center);
    this.view.rotation(this.yaw);
    this.view.translate(this.center);
  }

  @Override
  public void get(final Shader.UniformMat4 uniform) {
    uniform.set(this.view);
  }

  @Override
  public void get(final FloatBuffer buffer) {
    this.view.get(buffer);
  }

  @Override
  public void get(final int index, final FloatBuffer buffer) {
    this.view.get(index, buffer);
  }

  @Override
  public Vector3fc getPos() {
    return this.pos;
  }

  @Override
  public void getPos(final FloatBuffer buffer) {
    this.pos.get(buffer);
  }

  @Override
  public void getPos(final int index, final FloatBuffer buffer) {
    this.pos.get(index, buffer);
  }
}
