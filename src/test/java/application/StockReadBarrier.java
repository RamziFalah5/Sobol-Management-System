package application;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/** IT-fork-only timing seam. All SQL still executes against real MySQL; no DAO result is mocked. */
final class StockReadBarrier implements Driver, AutoCloseable {
    private final Driver delegate;
    private final List<Driver> displaced = new ArrayList<>();
    private final CyclicBarrier barrier = new CyclicBarrier(2);
    private final AtomicInteger reads = new AtomicInteger();
    private final String url;

    StockReadBarrier(String url) throws SQLException {
        this.url = url;
        for (Driver d : Collections.list(DriverManager.getDrivers())) if (d.acceptsURL(url)) displaced.add(d);
        if (displaced.isEmpty()) throw new SQLException("No real MySQL driver available for timing seam.");
        delegate = displaced.getFirst();
        for (Driver d : displaced) DriverManager.deregisterDriver(d);
        DriverManager.registerDriver(this);
    }

    int synchronizedReads() { return reads.get(); }
    public boolean acceptsURL(String candidate) { return url.equals(candidate); }
    public Connection connect(String candidate, Properties properties) throws SQLException {
        if (!acceptsURL(candidate)) return null;
        Properties safe = new Properties(); safe.putAll(properties);
        safe.setProperty("connectTimeout", "5000"); safe.setProperty("socketTimeout", "15000");
        Connection real = delegate.connect(candidate, safe);
        return proxy(Connection.class, real, (method, args, result) -> {
            if (method.getName().equals("prepareStatement") && args[0] instanceof String sql
                    && sql.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT)
                    .matches("select quantity, selling_price from product where product_id\\s*=\\s*\\?")) {
                return proxy(PreparedStatement.class, result, (m, a, r) -> {
                    if (m.getName().equals("executeQuery") && reads.incrementAndGet() <= 2) {
                        try { barrier.await(10, TimeUnit.SECONDS); }
                        catch (Exception failure) { throw new SQLException("Stock-read synchronization failed."); }
                    }
                    return r;
                });
            }
            return result;
        });
    }

    @FunctionalInterface private interface AfterCall { Object apply(Method m, Object[] args, Object result) throws Throwable; }
    private static <T> T proxy(Class<T> type, Object real, AfterCall after) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> {
            try { return after.apply(m, a, m.invoke(real, a)); }
            catch (InvocationTargetException failure) { throw failure.getCause(); }
        }));
    }

    @Override public void close() throws SQLException {
        DriverManager.deregisterDriver(this);
        for (Driver d : displaced) DriverManager.registerDriver(d);
    }
    public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException { return delegate.getPropertyInfo(u, p); }
    public int getMajorVersion() { return delegate.getMajorVersion(); }
    public int getMinorVersion() { return delegate.getMinorVersion(); }
    public boolean jdbcCompliant() { return delegate.jdbcCompliant(); }
    public Logger getParentLogger() { return Logger.getGlobal(); }
}
