package data.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayDeque;
import io.github.cdimascio.dotenv.Dotenv;


public class dbconnect {
    // DONT CHANGE ANYTHING HERE
    // DO NOT SHARE THESE DETAILS, THESE ARE CONFIDENTIAL AND ONLY FOR THE GROUP ONLY
    // //----- TURN THESE TO COMMENT IF WANT NYO MAGTESTING2 SA LOCAL HOST -----
    private static final Dotenv dotenv = Dotenv.load();
    
    private static final String URL = dotenv.get("DB_URL");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");
             
    //---- UNCOMMENT THESE IF YOU WANT TO TEST SA LOCAL HOST -----
    // private static final String URL =
    //         "jdbc:mysql://localhost:3306/defaultdb";
    // private static final String USER = "root";
    // private static final String PASSWORD = "root";

    // ---------------------------------------------------------------
    // Connection pool. Opening a connection to the remote database takes
    // about a second (TCP + TLS + login handshake), so finished
    // connections are parked here and handed out again instead of being
    // torn down - this is what keeps the kiosk responsive.
    // ---------------------------------------------------------------

    /** Connections waiting to be reused, newest last. */
    private static final ArrayDeque<Parked> IDLE = new ArrayDeque<>();
    /** How many parked connections to keep alive. */
    private static final int MAX_IDLE = 4;
    /** Connections parked for less than this are trusted without a ping. */
    private static final long TRUST_MILLIS = 5_000;

    /**
     * Returns a connection to the database. Closing it (also via
     * try-with-resources) parks it for the next caller instead of tearing
     * it down, so follow-up queries skip the ~1 s remote handshake.
     */
    public static Connection getConnection() throws SQLException {
        synchronized (IDLE) {
            Parked parked = IDLE.poll();
            if (parked != null) {
                if (System.currentTimeMillis() - parked.parkedAt < TRUST_MILLIS
                        || parked.conn.isValid(1)) {
                    return shield(parked.conn);
                }
                parked.conn.close();   // it died while parked: drop it
            }
        }
        return shield(DriverManager.getConnection(URL, USER, PASSWORD));
    }

    /**
     * Wraps the real connection so that close() only parks it; every other
     * call is forwarded unchanged. Each wrapper is checked out exclusively,
     * so the UI thread and DbWatcher never share one connection at a time.
     *
     * Proxy pattern: the returned object is a dynamic proxy with the same
     * {@link Connection} interface. Callers use it exactly like a real
     * connection; the proxy quietly intercepts close() to recycle it.
     */
    private static Connection shield(Connection real) {
        return (Connection) Proxy.newProxyInstance(
            dbconnect.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if (method.getName().equals("close")) {
                    // Never leave a half-open transaction in the pool.
                    if (!real.getAutoCommit()) {
                        real.rollback();
                        real.setAutoCommit(true);
                    }
                    synchronized (IDLE) {
                        if (IDLE.size() < MAX_IDLE) {
                            IDLE.addLast(new Parked(real));
                            return null;
                        }
                    }
                    real.close();   // pool is full: really close it
                    return null;
                }
                try {
                    return method.invoke(real, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();   // report the real SQL exception
                }
            });
    }

    /** A parked connection plus the time it was parked. */
    private static final class Parked {
        final Connection conn;
        final long parkedAt = System.currentTimeMillis();
        Parked(Connection conn) {
            this.conn = conn;
        }
    }
}
