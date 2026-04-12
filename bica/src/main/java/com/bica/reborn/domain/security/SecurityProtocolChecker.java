package com.bica.reborn.domain.security;

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
 * Security protocol verification via lattice properties.
 *
 * <p>Models security protocols (OAuth 2.0, TLS, mTLS, Kerberos, SAML) as
 * session types and verifies their correctness through lattice analysis
 * and subtyping for version evolution.
 *
 * <p>Ported from {@code reticulate/reticulate/security_protocols.py} (Step 89).
 */
public final class SecurityProtocolChecker {

    private SecurityProtocolChecker() {}

    // -----------------------------------------------------------------------
    // Data types
    // -----------------------------------------------------------------------

    /**
     * A named security protocol modelled as a session type.
     */
    public record SecurityProtocol(
            String name,
            List<String> roles,
            String sessionTypeString,
            List<String> securityProperties,
            String description) {

        public SecurityProtocol {
            Objects.requireNonNull(name);
            Objects.requireNonNull(sessionTypeString);
            roles = List.copyOf(roles);
            securityProperties = List.copyOf(securityProperties);
        }
    }

    /**
     * Complete analysis result for a security protocol.
     */
    public record SecurityAnalysisResult(
            SecurityProtocol protocol,
            SessionType ast,
            StateSpace stateSpace,
            LatticeResult latticeResult,
            int numStates,
            int numTransitions,
            boolean isWellFormed) {}

    /**
     * Result of checking backward compatibility between protocol versions.
     */
    public record ProtocolEvolutionResult(
            SecurityProtocol oldProtocol,
            SecurityProtocol newProtocol,
            boolean isBackwardCompatible) {}

    // -----------------------------------------------------------------------
    // Protocol definitions
    // -----------------------------------------------------------------------

    /** OAuth 2.0 Authorization Code flow (RFC 6749 Section 4.1). */
    public static SecurityProtocol oauth2AuthCode() {
        return new SecurityProtocol(
                "OAuth2AuthCode",
                List.of("Client", "AuthorizationServer", "ResourceOwner"),
                "&{requestAuth: &{authenticate: +{GRANTED: "
                        + "&{issueCode: &{exchangeCode: +{TOKEN_OK: end, "
                        + "TOKEN_ERROR: end}}}, DENIED: end}}}",
                List.of("authentication", "authorization", "token_confidentiality", "csrf_protection"),
                "OAuth 2.0 Authorization Code Grant."
        );
    }

    /** OAuth 2.0 Client Credentials flow (RFC 6749 Section 4.4). */
    public static SecurityProtocol oauth2ClientCredentials() {
        return new SecurityProtocol(
                "OAuth2ClientCredentials",
                List.of("Client", "AuthorizationServer"),
                "&{presentCredentials: &{validateClient: "
                        + "+{VALID: &{issueToken: end}, INVALID: end}}}",
                List.of("client_authentication", "token_confidentiality", "credential_protection"),
                "OAuth 2.0 Client Credentials Grant."
        );
    }

    /** TLS 1.3 handshake protocol (RFC 8446). */
    public static SecurityProtocol tls13Handshake() {
        return new SecurityProtocol(
                "TLS13Handshake",
                List.of("Client", "Server"),
                "&{clientHello: &{serverHello: &{serverCert: "
                        + "+{CERT_VALID: &{serverFinished: &{clientFinished: end}}, "
                        + "CERT_INVALID: end}}}}",
                List.of("confidentiality", "integrity", "server_authentication", "forward_secrecy"),
                "TLS 1.3 Handshake."
        );
    }

    /** Mutual TLS handshake with client certificate (RFC 8446 Section 4.4.2). */
    public static SecurityProtocol mutualTls() {
        return new SecurityProtocol(
                "MutualTLS",
                List.of("Client", "Server"),
                "&{clientHello: &{serverHello: &{serverCert: "
                        + "+{CERT_VALID: &{certRequest: &{clientCert: "
                        + "+{CLIENT_VALID: &{serverFinished: &{clientFinished: end}}, "
                        + "CLIENT_INVALID: end}}}, CERT_INVALID: end}}}}",
                List.of("confidentiality", "integrity", "mutual_authentication", "forward_secrecy"),
                "Mutual TLS with client certificate authentication."
        );
    }

    /** Kerberos authentication protocol (RFC 4120). */
    public static SecurityProtocol kerberosAuth() {
        return new SecurityProtocol(
                "Kerberos",
                List.of("Client", "KDC", "ServiceServer"),
                "&{asReq: +{AS_OK: &{asRep: &{tgsReq: "
                        + "+{TGS_OK: &{tgsRep: &{apReq: "
                        + "+{AP_OK: &{apRep: end}, AP_FAIL: end}}}, "
                        + "TGS_FAIL: end}}}, AS_FAIL: end}}",
                List.of("mutual_authentication", "single_sign_on", "ticket_delegation", "replay_protection"),
                "Kerberos authentication: AS Exchange, TGS Exchange, AP Exchange."
        );
    }

    /** SAML 2.0 Single Sign-On flow. */
    public static SecurityProtocol samlSso() {
        return new SecurityProtocol(
                "SAML_SSO",
                List.of("User", "ServiceProvider", "IdentityProvider"),
                "&{accessSP: &{redirectToIdP: &{authenticateIdP: "
                        + "+{AUTH_OK: &{issueAssertion: &{validateAssertion: "
                        + "+{VALID_ASSERTION: &{grantAccess: end}, "
                        + "INVALID_ASSERTION: end}}}, AUTH_FAIL: end}}}}",
                List.of("single_sign_on", "identity_federation", "assertion_integrity", "replay_protection"),
                "SAML 2.0 SSO flow."
        );
    }

    /** OAuth 2.0 Authorization Code with PKCE (RFC 7636). */
    public static SecurityProtocol oauth2Pkce() {
        return new SecurityProtocol(
                "OAuth2PKCE",
                List.of("Client", "AuthorizationServer", "ResourceOwner"),
                "&{generateChallenge: &{requestAuth: &{authenticate: "
                        + "+{GRANTED: &{issueCode: &{exchangeCodeWithVerifier: "
                        + "+{TOKEN_OK: end, TOKEN_ERROR: end}}}, DENIED: end}}}}",
                List.of("authentication", "authorization", "token_confidentiality", "code_interception_protection"),
                "OAuth 2.0 with PKCE."
        );
    }

    /** OpenID Connect Authorization Code Flow. */
    public static SecurityProtocol oidcAuthCode() {
        return new SecurityProtocol(
                "OIDCAuthCode",
                List.of("RelyingParty", "OpenIDProvider", "EndUser"),
                "&{requestAuthOpenID: &{authenticateUser: "
                        + "+{GRANTED: &{issueCode: &{exchangeForTokens: "
                        + "&{validateIDToken: +{CLAIMS_VALID: end, "
                        + "CLAIMS_INVALID: end}}}}, DENIED: end}}}",
                List.of("authentication", "identity_verification", "token_confidentiality", "nonce_protection"),
                "OpenID Connect Auth Code flow."
        );
    }

    /** All pre-defined security protocols. */
    public static List<SecurityProtocol> allProtocols() {
        return List.of(
                oauth2AuthCode(), oauth2ClientCredentials(), oauth2Pkce(),
                oidcAuthCode(), tls13Handshake(), mutualTls(),
                kerberosAuth(), samlSso());
    }

    // -----------------------------------------------------------------------
    // Verification
    // -----------------------------------------------------------------------

    /** Run the full verification pipeline on a security protocol. */
    public static SecurityAnalysisResult verifyProtocol(SecurityProtocol protocol) {
        SessionType ast = Parser.parse(protocol.sessionTypeString());
        StateSpace ss = StateSpaceBuilder.build(ast);
        LatticeResult lr = LatticeChecker.checkLattice(ss);

        return new SecurityAnalysisResult(
                protocol, ast, ss, lr,
                ss.states().size(), ss.transitions().size(),
                lr.isLattice());
    }

    /** Verify all pre-defined security protocols. */
    public static List<SecurityAnalysisResult> verifyAllProtocols() {
        return allProtocols().stream()
                .map(SecurityProtocolChecker::verifyProtocol)
                .toList();
    }

    /**
     * Check whether a new protocol version is backward-compatible.
     * Uses Gay-Hole subtyping: old &lt;= new.
     */
    public static ProtocolEvolutionResult checkEvolution(
            SecurityProtocol oldProto, SecurityProtocol newProto) {
        SessionType oldAst = Parser.parse(oldProto.sessionTypeString());
        SessionType newAst = Parser.parse(newProto.sessionTypeString());

        boolean compatible = SubtypingChecker.isSubtype(oldAst, newAst);

        return new ProtocolEvolutionResult(oldProto, newProto, compatible);
    }
}
