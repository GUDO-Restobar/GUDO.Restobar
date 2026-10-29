package util;

public class OrderSession {
    private static String orderType = "Dine-in";
    private static String paymentMethod = "Cash";
    private static int orderID = -1;

    public static void setOrderType(String type) {
        orderType = type;
    }

    public static String getOrderType() {
        return orderType;
    }

    public static void setPaymentMethod(String method) {
        paymentMethod = method;
    }

    public static String getPaymentMethod() {
        return paymentMethod;
    }

    public static void setOrderID(int id) {
        orderID = id;
    }

    public static int getOrderID() {
        return orderID;
    }
}