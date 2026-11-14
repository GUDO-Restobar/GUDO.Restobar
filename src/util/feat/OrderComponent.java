package util.feat;

/**
 * Anything that has a price inside an order.
 *
 * Composite pattern: {@link OrderLine} is the leaf (one meal with its add-ons)
 * and {@link Cart} is the composite (a group of lines). Both are treated the
 * same way through {@link #getSubtotal()}, so a screen can ask either one what
 * it costs without caring whether it holds one line or the whole order.
 */
public interface OrderComponent {

    /** The price of this part of the order, in pesos. */
    double getSubtotal();
}
