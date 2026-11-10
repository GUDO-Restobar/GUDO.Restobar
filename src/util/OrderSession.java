package util;

/**
 * The one kiosk session (Singleton pattern).
 *
 * Main purpose: remembers the choices made on the way through the kiosk - the
 * order type (Dine-in / Takeout), the payment method (Cash / Cashless) and
 * the id of the order that was last saved - so the screens and KioskMain can
 * read them without passing them around.
 *
 * The private constructor plus the single INSTANCE guarantee that there is
 * always exactly one session, no matter which screen asks for it. Screens
 * reach it through {@link #getInstance()}.
 */
public class OrderSession {

    /** The one and only session. */
    private static final OrderSession INSTANCE = new OrderSession();

    /** What the customer is ordering: "Dine-in" or "Takeout". */
    private String orderType = "Dine-in";
    /** How the customer chose to pay: "Cash" or "Cashless". */
    private String paymentMethod = "Cash";
    /** The id of the order that was saved last, or -1 if none was. */
    private int orderID = -1;

    /** Singleton: nobody outside this class can create a second session. */
    private OrderSession() {
    }

    /** The shared session instance. */
    public static OrderSession getInstance() {
        return INSTANCE;
    }

    public void setOrderType(String type) {
        this.orderType = type;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setPaymentMethod(String method) {
        this.paymentMethod = method;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setOrderID(int id) {
        this.orderID = id;
    }

    public int getOrderID() {
        return orderID;
    }
}
