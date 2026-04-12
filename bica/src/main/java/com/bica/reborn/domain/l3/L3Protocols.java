package com.bica.reborn.domain.l3;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Level 3 (full fidelity) session type models for real-world protocols.
 *
 * <p>Three levels of session type modeling fidelity:
 * <ul>
 *   <li>L1 — Flat branch: all methods as independent choices (over-permissive)
 *   <li>L2 — Trace mining: observed sequences only (over-restrictive)
 *   <li>L3 — Full protocol: methods + return-value selections + preconditions
 * </ul>
 *
 * <p>Ported from {@code reticulate/reticulate/l3_protocols.py} (Step 97d).
 */
public final class L3Protocols {

    private L3Protocols() {}

    /**
     * A protocol specification at multiple fidelity levels.
     *
     * @param description Human-readable description.
     * @param l3          Full-fidelity session type string.
     * @param l1          Flat-branch (over-permissive) session type string.
     */
    public record ProtocolSpec(String description, String l3, String l1) {}

    /** All L3 protocol catalog entries, keyed by protocol name. */
    public static final Map<String, ProtocolSpec> CATALOG;

    static {
        var map = new LinkedHashMap<String, ProtocolSpec>();

        // --- Java protocols ---

        map.put("java_iterator", new ProtocolSpec(
                "java.util.Iterator<E> — hasNext selects TRUE/FALSE",
                "rec X . &{hasNext: +{TRUE: &{next: X}, FALSE: end}}",
                "&{hasNext: end, next: end, remove: end}"));

        map.put("jdbc_connection", new ProtocolSpec(
                "java.sql.Connection — createStatement then execute with result selection",
                "&{createStatement: &{executeQuery: rec X . &{next: +{TRUE: &{getString: X}, FALSE: &{close: &{commit: &{close: end}}}}}}, close: end}",
                "&{createStatement: end, executeQuery: end, executeUpdate: end, commit: end, rollback: end, close: end}"));

        map.put("java_inputstream", new ProtocolSpec(
                "java.io.InputStream — available() selects, read returns data or EOF",
                "&{open: rec X . &{available: +{TRUE: &{read: +{data: X, EOF: &{close: end}}, skip: X}, FALSE: &{close: end}}, close: end}}",
                "&{open: end, read: end, skip: end, available: end, mark: end, reset: end, close: end}"));

        map.put("java_socket", new ProtocolSpec(
                "java.net.Socket — connect selects success/failure",
                "&{connect: +{OK: &{getInputStream: &{getOutputStream: rec X . &{read: +{data: X, EOF: &{close: end}}, write: X, close: end}}}, FAIL: end}, close: end}",
                "&{connect: end, getInputStream: end, getOutputStream: end, close: end}"));

        map.put("java_httpurlconnection", new ProtocolSpec(
                "HttpURLConnection — connect then response code selection",
                "&{setRequestMethod: &{connect: +{OK: &{getResponseCode: +{s200: &{getInputStream: &{read: &{disconnect: end}}}, s404: &{disconnect: end}, s500: &{disconnect: end}}}, FAIL: &{disconnect: end}}}}",
                "&{setRequestMethod: end, connect: end, getResponseCode: end, getInputStream: end, getOutputStream: end, disconnect: end}"));

        map.put("java_servlet", new ProtocolSpec(
                "HttpServlet — init then service dispatches to doGet/doPost/doPut",
                "&{init: rec X . &{service: +{GET: &{doGet: X}, POST: &{doPost: X}, PUT: &{doPut: X}, DELETE: &{doDelete: X}}, destroy: end}}",
                "&{init: end, service: end, doGet: end, doPost: end, doPut: end, doDelete: end, destroy: end}"));

        map.put("java_outputstream", new ProtocolSpec(
                "java.io.OutputStream — write loop, flush, close",
                "rec X . &{write: X, flush: X, close: end}",
                "&{write: end, flush: end, close: end}"));

        map.put("java_lock", new ProtocolSpec(
                "java.util.concurrent.locks.Lock — acquire/release cycle",
                "rec X . &{lock: &{unlock: X}, tryLock: +{TRUE: &{unlock: X}, FALSE: end}}",
                "&{lock: end, unlock: end, tryLock: end, lockInterruptibly: end}"));

        map.put("java_executorservice", new ProtocolSpec(
                "ExecutorService — submit tasks then shutdown",
                "rec X . &{submit: X, execute: X, shutdown: &{awaitTermination: +{TRUE: end, FALSE: end}}, shutdownNow: end}",
                "&{submit: end, execute: end, shutdown: end, shutdownNow: end, awaitTermination: end, invokeAll: end}"));

        map.put("java_bufferedreader", new ProtocolSpec(
                "java.io.BufferedReader — readLine returns data or null (EOF)",
                "&{open: rec X . &{readLine: +{data: X, EOF: &{close: end}}, close: end}}",
                "&{open: end, readLine: end, read: end, ready: end, close: end}"));

        // --- Python protocols ---

        map.put("python_file", new ProtocolSpec(
                "Python file object — open/read/write with EOF selection",
                "&{open: rec X . &{read: +{data: X, EOF: &{close: end}}, write: X, close: end}}",
                "&{open: end, read: end, write: end, readline: end, seek: end, flush: end, close: end}"));

        map.put("python_sqlite3", new ProtocolSpec(
                "sqlite3.Connection — cursor/execute/fetch cycle",
                "&{connect: &{cursor: rec X . &{execute: &{fetchone: +{row: X, none: &{commit: &{close: end}}}, fetchall: &{commit: &{close: end}}}, close: &{close: end}}}}",
                "&{connect: end, cursor: end, execute: end, fetchone: end, fetchall: end, commit: end, rollback: end, close: end}"));

        map.put("python_http", new ProtocolSpec(
                "http.client.HTTPConnection — request/response cycle",
                "&{connect: rec X . &{request: &{getresponse: +{s200: &{read: X}, s404: X, s500: X}}, close: end}}",
                "&{connect: end, request: end, getresponse: end, read: end, close: end}"));

        map.put("python_socket", new ProtocolSpec(
                "socket.socket — server or client mode",
                "&{create: +{server: &{bind: &{listen: rec X . &{accept: &{recv: &{send: X}}, close: end}}}, client: &{connect: rec Y . &{send: &{recv: Y}, close: end}}}}",
                "&{create: end, bind: end, listen: end, accept: end, connect: end, send: end, recv: end, close: end}"));

        map.put("python_iterator", new ProtocolSpec(
                "Python iterator protocol — __next__ raises StopIteration",
                "rec X . &{next: +{value: X, StopIteration: end}}",
                "&{iter: end, next: end}"));

        map.put("python_contextmanager", new ProtocolSpec(
                "Context manager — __enter__/__exit__ with exception selection",
                "&{enter: rec X . &{use: X, exit: +{OK: end, EXCEPTION: end}}}",
                "&{enter: end, use: end, exit: end}"));

        CATALOG = Collections.unmodifiableMap(map);
    }
}
