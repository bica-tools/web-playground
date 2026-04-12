package com.bica.reborn.domain.smartcontract;

import com.bica.reborn.lattice.LatticeChecker;
import com.bica.reborn.lattice.LatticeResult;
import com.bica.reborn.parser.Parser;
import com.bica.reborn.ast.SessionType;
import com.bica.reborn.statespace.StateSpace;
import com.bica.reborn.statespace.StateSpaceBuilder;

import java.util.List;
import java.util.Objects;

/**
 * Smart contract lifecycle verification via session types.
 *
 * <p>Models blockchain smart contract state machines as session types,
 * enabling formal verification of contract correctness through lattice
 * analysis and conformance testing.
 *
 * <p>Ported from {@code reticulate/reticulate/smart_contract.py} (Step 81).
 */
public final class SmartContractChecker {

    private SmartContractChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /**
     * A named state in a smart contract lifecycle.
     */
    public record SmartContractState(
            String name,
            List<String> allowedTransitions,
            boolean requiresAuth) {

        public SmartContractState {
            Objects.requireNonNull(name);
            allowedTransitions = List.copyOf(allowedTransitions);
        }

        public SmartContractState(String name, List<String> allowedTransitions) {
            this(name, allowedTransitions, false);
        }
    }

    /**
     * A smart contract lifecycle modelled as a session type.
     */
    public record SmartContractWorkflow(
            String name,
            String standard,
            List<SmartContractState> states,
            String sessionTypeString,
            String description,
            List<String> solidityEvents) {

        public SmartContractWorkflow {
            Objects.requireNonNull(name);
            Objects.requireNonNull(sessionTypeString);
            states = List.copyOf(states);
            solidityEvents = List.copyOf(solidityEvents);
        }
    }

    /**
     * Complete analysis result for a smart contract lifecycle.
     */
    public record ContractAnalysisResult(
            SmartContractWorkflow workflow,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            int numStates,
            int numTransitions,
            boolean isWellFormed) {}

    // -----------------------------------------------------------------------
    // Contract lifecycle definitions
    // -----------------------------------------------------------------------

    /** ERC-20 fungible token lifecycle. */
    public static SmartContractWorkflow erc20Lifecycle() {
        return new SmartContractWorkflow(
                "ERC20Token", "ERC-20",
                List.of(
                        new SmartContractState("Undeployed", List.of("deploy"), true),
                        new SmartContractState("Active", List.of("approve", "transfer", "burn")),
                        new SmartContractState("Approved", List.of("transferFrom", "transfer", "burn")),
                        new SmartContractState("Burned", List.of())),
                "&{deploy: rec X . &{approve: &{transferFrom: X, "
                        + "transfer: X, burn: end}, "
                        + "transfer: X, burn: end}}",
                "ERC-20 fungible token lifecycle: deploy, approve, transfer, burn.",
                List.of("Transfer", "Approval")
        );
    }

    /** ERC-721 non-fungible token lifecycle. */
    public static SmartContractWorkflow erc721Lifecycle() {
        return new SmartContractWorkflow(
                "ERC721Token", "ERC-721",
                List.of(
                        new SmartContractState("Undeployed", List.of("deploy"), true),
                        new SmartContractState("Deployed", List.of("mint"), true),
                        new SmartContractState("Minted", List.of("transfer", "approve", "burn")),
                        new SmartContractState("Approved", List.of("transferFrom", "transfer", "burn")),
                        new SmartContractState("Burned", List.of())),
                "&{deploy: &{mint: rec X . &{transfer: X, "
                        + "approve: &{transferFrom: X, transfer: X, burn: end}, "
                        + "burn: end}}}",
                "ERC-721 non-fungible token lifecycle: deploy, mint, transfer, approve, burn.",
                List.of("Transfer", "Approval", "ApprovalForAll")
        );
    }

    /** DeFi lending pool lifecycle. */
    public static SmartContractWorkflow defiLending() {
        return new SmartContractWorkflow(
                "DeFiLending", "DeFi-Lending",
                List.of(
                        new SmartContractState("Empty", List.of("deposit")),
                        new SmartContractState("Funded", List.of("borrow", "withdraw")),
                        new SmartContractState("Borrowed", List.of("repay", "liquidate")),
                        new SmartContractState("Repaid", List.of("withdraw", "borrow")),
                        new SmartContractState("Liquidated", List.of()),
                        new SmartContractState("Withdrawn", List.of())),
                "&{deposit: rec X . &{borrow: +{HEALTHY: &{repay: X}, "
                        + "UNDERCOLLATERALIZED: &{liquidate: end}}, "
                        + "withdraw: end}}",
                "DeFi lending pool lifecycle: deposit, borrow, repay, withdraw, liquidate.",
                List.of("Deposit", "Borrow", "Repay", "Withdraw", "Liquidation")
        );
    }

    /** Multi-signature wallet lifecycle. */
    public static SmartContractWorkflow multisigWallet() {
        return new SmartContractWorkflow(
                "MultisigWallet", "Multisig",
                List.of(
                        new SmartContractState("Idle", List.of("propose")),
                        new SmartContractState("Proposed", List.of("approve", "reject")),
                        new SmartContractState("QuorumReached", List.of("execute")),
                        new SmartContractState("Executed", List.of()),
                        new SmartContractState("Rejected", List.of())),
                "rec X . &{propose: +{QUORUM_REACHED: &{execute: X}, "
                        + "QUORUM_NOT_REACHED: &{approve: +{QUORUM_REACHED: "
                        + "&{execute: X}, QUORUM_NOT_REACHED: end}}, "
                        + "REJECTED: end}}",
                "Multi-signature wallet: propose, approve, execute, reject.",
                List.of("Submission", "Confirmation", "Execution", "Revocation")
        );
    }

    /** Auction contract lifecycle. */
    public static SmartContractWorkflow auctionContract() {
        return new SmartContractWorkflow(
                "Auction", "Auction",
                List.of(
                        new SmartContractState("Created", List.of("start"), true),
                        new SmartContractState("Active", List.of("bid", "endAuction")),
                        new SmartContractState("Ended", List.of("withdraw")),
                        new SmartContractState("Settled", List.of())),
                "&{start: rec X . &{bid: X, endAuction: &{withdraw: end}}}",
                "English auction lifecycle: start, bid, end, withdraw.",
                List.of("AuctionStarted", "BidPlaced", "AuctionEnded", "FundsWithdrawn")
        );
    }

    /** All pre-defined contract workflows. */
    public static List<SmartContractWorkflow> allWorkflows() {
        return List.of(
                erc20Lifecycle(), erc721Lifecycle(), defiLending(),
                multisigWallet(), auctionContract());
    }

    // -----------------------------------------------------------------------
    // Verification
    // -----------------------------------------------------------------------

    /** Run the full verification pipeline on a smart contract lifecycle. */
    public static ContractAnalysisResult verifyContract(SmartContractWorkflow workflow) {
        SessionType ast = Parser.parse(workflow.sessionTypeString());
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        return new ContractAnalysisResult(
                workflow, ast, ss, lr,
                ss.states().size(), ss.transitions().size(),
                lr.isLattice());
    }

    /** Verify all pre-defined contract workflows. */
    public static List<ContractAnalysisResult> verifyAllContracts() {
        return allWorkflows().stream()
                .map(SmartContractChecker::verifyContract)
                .toList();
    }
}
