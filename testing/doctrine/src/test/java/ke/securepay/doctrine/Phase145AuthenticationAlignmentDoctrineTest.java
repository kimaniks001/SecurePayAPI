package ke.securepay.doctrine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Phase145AuthenticationAlignmentDoctrineTest {

    private static final Path ROOT = locateRepositoryRoot();

    @Test
    void authenticationDoctrineLocksKsNumberFirstMfaSequence()
            throws IOException {
        String doctrine = read(
                "docs/domains/AUTHENTICATION_DOCTRINE.md");

        assertTrue(doctrine.contains(
                "KS Number → password → OTP → authenticated session"));
        assertTrue(doctrine.contains(
                "Password verification alone never creates an active"
                        + " authenticated session"));
        assertTrue(doctrine.contains(
                "Session activation requires trusted proof"));
        assertTrue(doctrine.contains(
                "Phone and email do not replace the KS Number"));
    }

    @Test
    void authenticationDoctrineLocksLifecycleAndReplaySafety()
            throws IOException {
        String doctrine = read(
                "docs/domains/AUTHENTICATION_DOCTRINE.md");

        assertTrue(doctrine.contains(
                "Normal authentication requires both an ACTIVE identity"
                        + " and an active credential"));
        assertTrue(doctrine.contains(
                "SUSPENDED and CLOSED identities cannot authenticate"));
        assertTrue(doctrine.contains(
                "Replay-triggered revocation must commit"));
        assertTrue(doctrine.contains(
                "Password change revokes all sessions and refresh tokens"));
    }

    @Test
    void phase15ContractPreventsPasswordOnlySessionIssuance()
            throws IOException {
        String contract = read(
                "docs/architecture/PHASE_15_AUTHENTICATION_IMPLEMENTATION_CONTRACT.md");

        assertTrue(contract.contains(
                "password success alone cannot issue an active session"));
        assertTrue(contract.contains(
                "replay of a rotated refresh token commits session revocation"));
        assertTrue(contract.contains(
                "raw secrets are absent from persistence"));
    }

    @Test
    void publicIdentityProjectionExcludesPrivateSecurityData()
            throws IOException {
        String standard = read(
                "docs/architecture/IDENTITY_ENDPOINT_EXPOSURE_STANDARD.md");

        assertTrue(standard.contains("phone or email"));
        assertTrue(standard.contains("credential state"));
        assertTrue(standard.contains(
                "bank, virtual-account or settlement mappings"));
        assertTrue(standard.contains(
                "privileged identity mutations must remain behind"
                        + " a trusted internal boundary"));
    }

    @Test
    void actorIdentityCannotComeFromUntrustedHeaders()
            throws IOException {
        String standard = read(
                "docs/security/AUTHENTICATION_AND_SESSION_SECURITY_STANDARD.md");

        assertTrue(standard.contains(
                "Clients cannot establish trusted actor identity "
                        + "using request headers"));
        assertTrue(standard.contains(
                "Phase 17 roles, organizations, scopes and delegated authority"));
        assertTrue(standard.contains("Future compatibility"));
    }

    private static String read(String relativePath)
            throws IOException {
        return Files.readString(ROOT.resolve(relativePath));
    }

    private static Path locateRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();

        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))
                    && Files.isDirectory(current.resolve("docs"))) {
                return current;
            }
            current = current.getParent();
        }

        throw new IllegalStateException(
                "Unable to locate SecurePay repository root");
    }
}
