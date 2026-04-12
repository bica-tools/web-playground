package com.bica.reborn.domain.database;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;
import com.bica.reborn.subtyping.SubtypingChecker;

import java.util.List;
import java.util.Objects;

/**
 * Database transaction protocol verification via session types.
 *
 * <p>Models database transaction protocols as session types, enabling formal
 * verification through lattice analysis.
 *
 * <p>Ported from {@code reticulate/reticulate/db_protocols.py} (Step 90).
 */
public final class DBProtocolChecker {

    private DBProtocolChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /**
     * A named database protocol modelled as a session type.
     */
    public record DBProtocol(
            String name,
            String sessionTypeString,
            String isolationLevel,
            String description,
            List<String> participants,
            List<String> properties) {

        public DBProtocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(sessionTypeString);
            Objects.requireNonNull(isolationLevel);
            participants = List.copyOf(participants);
            properties = List.copyOf(properties);
        }
    }

    /**
     * Complete analysis result for a database protocol.
     */
    public record DBAnalysisResult(
            DBProtocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            int numStates,
            int numTransitions,
            boolean isWellFormed) {}

    /**
     * Result of comparing protocol variants at different isolation levels.
     */
    public record IsolationComparisonResult(
            DBProtocol baseProtocol,
            DBProtocol strictProtocol,
            boolean isCompatible) {}

    // -----------------------------------------------------------------------
    // Protocol definitions
    // -----------------------------------------------------------------------

    /** JDBC Connection lifecycle protocol. */
    public static DBProtocol jdbcConnection() {
        return new DBProtocol(
                "JDBCConnection",
                "&{connect: &{createStatement: &{execute: "
                        + "+{SUCCESS: &{processResults: &{close: end}}, "
                        + "ERROR: &{close: end}}}}}",
                "READ_COMMITTED",
                "JDBC Connection lifecycle: connect, create statement, execute, process or handle error, close.",
                List.of("Client", "Database"),
                List.of("connection_safety", "resource_cleanup", "error_handling", "statement_ordering")
        );
    }

    /** JDBC transaction protocol with savepoints. */
    public static DBProtocol jdbcTransaction() {
        return new DBProtocol(
                "JDBCTransaction",
                "&{beginTx: &{executeOp: &{checkpoint: "
                        + "+{SAVEPOINT: &{executeMore: +{COMMIT: &{commitTx: end}, "
                        + "ROLLBACK: &{rollbackTx: end}}}, "
                        + "NO_SAVEPOINT: +{COMMIT: &{commitTx: end}, "
                        + "ROLLBACK: &{rollbackTx: end}}}}}}",
                "READ_COMMITTED",
                "JDBC transaction with savepoints.",
                List.of("Client", "TransactionManager"),
                List.of("atomicity", "isolation", "commit_rollback_exclusion", "savepoint_ordering")
        );
    }

    /** Connection pool acquire/use/release protocol. */
    public static DBProtocol connectionPool() {
        return new DBProtocol(
                "ConnectionPool",
                "&{requestConn: +{ACQUIRED: &{useConnection: "
                        + "&{executeQuery: &{releaseConn: &{validateReturn: end}}}}, "
                        + "TIMEOUT: end}}",
                "READ_COMMITTED",
                "Connection pool lifecycle: request, acquire/timeout, use, release, validate.",
                List.of("Client", "ConnectionPool", "Database"),
                List.of("resource_bounding", "timeout_safety", "connection_reuse", "leak_prevention")
        );
    }

    /** Prepared statement lifecycle protocol. */
    public static DBProtocol preparedStatement() {
        return new DBProtocol(
                "PreparedStatement",
                "&{prepare: &{bindParams: &{executePrepared: "
                        + "+{RESULTS: &{fetchRows: &{closePrepared: end}}, "
                        + "EXEC_ERROR: &{closePrepared: end}}}}}",
                "READ_COMMITTED",
                "Prepared statement lifecycle: prepare, bind, execute, fetch/error, close.",
                List.of("Client", "Database"),
                List.of("sql_injection_prevention", "parameter_binding_order", "statement_cleanup")
        );
    }

    /** Two-phase commit (2PC) protocol for distributed transactions. */
    public static DBProtocol twoPhaseCommit() {
        return new DBProtocol(
                "TwoPhaseCommit",
                "&{prepare: +{VOTE_YES: &{doCommit: &{ackCommit: end}}, "
                        + "VOTE_NO: &{doAbort: &{ackAbort: end}}}}",
                "SERIALIZABLE",
                "XA two-phase commit: prepare, vote, commit/abort, acknowledge.",
                List.of("Coordinator", "Participant"),
                List.of("atomicity", "distributed_consensus", "failure_recovery", "global_serializability")
        );
    }

    /** Database cursor iteration protocol. */
    public static DBProtocol cursorIteration() {
        return new DBProtocol(
                "CursorIteration",
                "&{openCursor: rec X . &{fetchNext: "
                        + "+{HAS_ROW: &{processRow: X}, "
                        + "NO_MORE_ROWS: &{closeCursor: end}}}}",
                "READ_COMMITTED",
                "Database cursor: open, iteratively fetch/process rows, close.",
                List.of("Client", "Database"),
                List.of("cursor_safety", "resource_cleanup", "iteration_ordering", "exhaustion_guarantee")
        );
    }

    /** Stored procedure call protocol. */
    public static DBProtocol storedProcedure() {
        return new DBProtocol(
                "StoredProcedure",
                "&{prepareCall: &{registerOutParams: &{setInParams: "
                        + "&{executeProc: +{PROC_OK: &{getOutParams: "
                        + "&{closeCallable: end}}, "
                        + "PROC_ERROR: &{closeCallable: end}}}}}}",
                "READ_COMMITTED",
                "Stored procedure call: prepare, register OUT, set IN, execute, retrieve, close.",
                List.of("Client", "Database"),
                List.of("parameter_registration_order", "callable_cleanup", "error_handling")
        );
    }

    /** Batch statement execution protocol. */
    public static DBProtocol batchExecution() {
        return new DBProtocol(
                "BatchExecution",
                "&{createBatch: &{addStatement: &{executeBatch: "
                        + "+{BATCH_OK: &{processCounts: &{clearBatch: end}}, "
                        + "BATCH_ERROR: &{clearBatch: end}}}}}",
                "READ_COMMITTED",
                "Batch execution: create, add, execute, process/error, clear.",
                List.of("Client", "Database"),
                List.of("batch_atomicity", "batch_ordering", "resource_cleanup", "error_reporting")
        );
    }

    /** All pre-defined database protocols. */
    public static List<DBProtocol> allProtocols() {
        return List.of(
                jdbcConnection(), jdbcTransaction(), connectionPool(),
                preparedStatement(), twoPhaseCommit(), cursorIteration(),
                storedProcedure(), batchExecution());
    }

    // -----------------------------------------------------------------------
    // Verification
    // -----------------------------------------------------------------------

    /** Run the full verification pipeline on a database protocol. */
    public static DBAnalysisResult verifyProtocol(DBProtocol protocol) {
        SessionType ast = Parser.parse(protocol.sessionTypeString());
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        return new DBAnalysisResult(
                protocol, ast, ss, lr,
                ss.states().size(), ss.transitions().size(),
                lr.isLattice());
    }

    /** Verify all pre-defined database protocols. */
    public static List<DBAnalysisResult> verifyAllProtocols() {
        return allProtocols().stream()
                .map(DBProtocolChecker::verifyProtocol)
                .toList();
    }

    /**
     * Compare two protocol variants at different isolation levels.
     * Uses Gay-Hole subtyping: base &lt;= strict.
     */
    public static IsolationComparisonResult compareIsolationLevels(
            DBProtocol base, DBProtocol strict) {
        SessionType baseAst = Parser.parse(base.sessionTypeString());
        SessionType strictAst = Parser.parse(strict.sessionTypeString());

        boolean compatible = SubtypingChecker.isSubtype(baseAst, strictAst);

        return new IsolationComparisonResult(base, strict, compatible);
    }
}
