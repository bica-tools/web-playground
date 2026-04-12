package com.bica.reborn.cli;

import java.util.*;

/**
 * Registry of known Java API protocols as session types.
 *
 * <p>Each protocol defines:
 * <ul>
 *   <li>A session type string</li>
 *   <li>Variable name patterns that typically hold this type</li>
 *   <li>Whether end-of-method check should be enforced (owned resources)</li>
 * </ul>
 *
 * <p>Usage: {@code bica scan <dir> --registry} scans with ALL protocols.
 */
public final class ProtocolRegistry {

    public record Protocol(
            String name,
            String sessionType,
            Set<String> varPatterns,
            boolean checkEnd,
            String description) {}

    private ProtocolRegistry() {}

    public static final List<Protocol> ALL = List.of(

        // --- JDBC ---

        new Protocol("JDBC Connection",
                "rec X . &{createStatement: X, prepareStatement: X, " +
                "commit: X, rollback: X, setAutoCommit: X, getAutoCommit: X, " +
                "getMetaData: X, close: end}",
                Set.of("conn", "connection", "c", "con", "dbConnection", "sqlConnection"),
                true,
                "java.sql.Connection: must close after use"),

        new Protocol("JDBC Statement",
                "rec X . &{executeQuery: X, executeUpdate: X, execute: X, " +
                "setQueryTimeout: X, close: end}",
                Set.of("stmt", "statement", "st", "preparedStatement", "ps", "pstmt"),
                true,
                "java.sql.Statement: must close after use"),

        new Protocol("JDBC ResultSet",
                "rec X . &{next: +{TRUE: &{getString: X, getInt: X, getLong: X, " +
                "getDouble: X, getObject: X, getBoolean: X, close: end}, " +
                "FALSE: &{close: end}}, close: end}",
                Set.of("rs", "resultSet", "results", "rset"),
                true,
                "java.sql.ResultSet: next() is a selection, must close"),

        // --- I/O Streams ---

        new Protocol("InputStream",
                "rec X . &{read: X, skip: X, available: X, mark: X, " +
                "reset: X, close: end}",
                Set.of("in", "input", "inputStream", "is", "fis", "bis", "dis",
                       "stream", "source"),
                true,
                "java.io.InputStream: must close after use"),

        new Protocol("OutputStream",
                "rec X . &{write: X, flush: X, close: end}",
                Set.of("out", "output", "outputStream", "os", "fos", "bos", "dos"),
                true,
                "java.io.OutputStream: must flush and close"),

        new Protocol("Reader",
                "rec X . &{read: X, readLine: X, ready: X, mark: X, " +
                "reset: X, close: end}",
                Set.of("reader", "br", "bufferedReader", "fileReader", "fr"),
                true,
                "java.io.Reader: must close after use"),

        new Protocol("Writer",
                "rec X . &{write: X, append: X, flush: X, close: end}",
                Set.of("writer", "bw", "bufferedWriter", "fileWriter", "fw", "pw",
                       "printWriter"),
                true,
                "java.io.Writer: must flush and close"),

        // --- Collections/Iterator ---

        new Protocol("Iterator",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                Set.of("iter", "iterator", "it"),
                false,
                "java.util.Iterator: hasNext() guards next()"),

        // --- Concurrency ---

        new Protocol("Lock",
                "rec X . &{lock: &{unlock: X}, tryLock: +{TRUE: &{unlock: X}, FALSE: end}}",
                Set.of("lock", "mutex", "reentrantLock"),
                false,
                "java.util.concurrent.locks.Lock: lock/unlock pairing"),

        new Protocol("ExecutorService",
                "rec X . &{submit: X, execute: X, shutdown: &{awaitTermination: end}, " +
                "shutdownNow: end}",
                Set.of("executor", "executorService", "pool", "threadPool", "service"),
                true,
                "ExecutorService: must shutdown after use"),

        // --- Network ---

        new Protocol("Socket",
                "rec X . &{connect: X, getInputStream: X, getOutputStream: X, " +
                "setSoTimeout: X, close: end}",
                Set.of("socket", "sock", "clientSocket", "serverSocket"),
                true,
                "java.net.Socket: must close after use"),

        new Protocol("HttpURLConnection",
                "rec X . &{setRequestMethod: X, setDoOutput: X, " +
                "addRequestProperty: X, setRequestProperty: X, " +
                "connect: X, getResponseCode: X, getInputStream: X, " +
                "getOutputStream: X, disconnect: end}",
                Set.of("httpConn", "urlConnection", "httpConnection", "huc"),
                true,
                "HttpURLConnection: must disconnect after use"),

        // --- Optional ---

        new Protocol("Optional",
                "&{isPresent: +{TRUE: &{get: end}, FALSE: end}, " +
                "orElse: end, orElseGet: end, orElseThrow: end, " +
                "ifPresent: end, isEmpty: end, map: end}",
                Set.of("opt", "optional", "result", "maybe"),
                false,
                "Optional: get() requires isPresent() check")
    );

    /** Look up a protocol by name. */
    public static Optional<Protocol> byName(String name) {
        return ALL.stream()
                .filter(p -> p.name().equalsIgnoreCase(name))
                .findFirst();
    }

    /** Get all variable patterns across all protocols. */
    public static Map<String, Protocol> varToProtocol() {
        var map = new LinkedHashMap<String, Protocol>();
        for (var p : ALL) {
            for (var v : p.varPatterns()) {
                map.put(v, p);
            }
        }
        return map;
    }

    /** List all protocol names. */
    public static List<String> names() {
        return ALL.stream().map(Protocol::name).toList();
    }
}
