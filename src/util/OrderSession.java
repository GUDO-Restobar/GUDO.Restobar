package util;

public class OrderSession {
    private static String orderType = "Dine-in";
    private static int orderID = -1;

    public static void setOrderType(String type) {
        orderType = type;
    }

    public static String getOrderType() {
        return orderType;
    }

    public static void setOrderID(int id) {
        orderID = id;
    }

    public static int getOrderID() {
        return orderID;
    }
}