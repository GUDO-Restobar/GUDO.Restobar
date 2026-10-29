package util;

/**
 * Prototype design pattern.
 *
 * An object that implements this interface knows how to produce an
 * independent copy of itself. Code that needs a "scratch" version of an
 * object (for example, a screen that lets the customer experiment with an
 * order and only commit the result when they press Save) can call copy()
 * instead of changing the original directly.
 */
public interface Prototype<T> {
    T copy();
}
