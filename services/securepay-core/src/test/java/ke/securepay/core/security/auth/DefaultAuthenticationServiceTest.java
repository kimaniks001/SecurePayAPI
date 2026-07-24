package ke.securepay.core.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import ke.securepay.core.security.auth.challenge.AuthenticationChallenge;
import ke.securepay.core.security.auth.challenge.AuthenticationChallengeRepository;
import ke.securepay.core.security.auth.challenge.AuthenticationChallengeStatus;
import ke.securepay.core.security.auth.credential.AuthenticationCredential;
import ke.securepay.core.security.auth.credential.AuthenticationCredentialRepository;
import ke.securepay.core.security.password.PasswordVerifier;
import ke.securepay.platform.identity.ksnumber.KsNumber;
import ke.securepay.platform.identity.model.IdentityStatus;
import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.identity.model.KsIdentityRecord;
import ke.securepay.platform.identity.service.KsIdentityQueryService;
import org.junit.jupiter.api.Test;

class DefaultAuthenticationServiceTest {

    private static final UUID IDENTITY_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW =
            Instant.parse("2026-07-24T12:00:00Z");
    private static final AuthenticateCommand COMMAND =
            new AuthenticateCommand(
                    "KS000001",
                    "correct-password",
                    "securepay-web",
                    "device-1",
                    "ip-hash-1");

    @Test
    void createsPendingMfaChallengeAfterPasswordVerification() {
        AtomicReference<AuthenticationChallenge> inserted =
                new AtomicReference<>();

        DefaultAuthenticationService service = service(
                Optional.of(activeCredential()),
                Optional.of(activeIdentity()),
                (rawPassword, passwordHash) ->
                        rawPassword.equals("correct-password")
                                && passwordHash.equals("stored-hash"),
                inserted);

        PendingAuthenticationResult result =
                service.authenticate(COMMAND);

        assertThat(result.challengeToken()).isEqualTo("raw-challenge-token");
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(result.principal()).isEqualTo(
                new AuthenticationPrincipal(
                        IDENTITY_ID.toString(),
                        "KS000001",
                        "James",
                        "PASSWORD",
                        "securepay-web",
                        "device-1",
                        "ip-hash-1"));

        AuthenticationChallenge challenge = inserted.get();
        assertThat(challenge).isNotNull();
        assertThat(challenge.id()).isEqualTo(result.challengeId());
        assertThat(challenge.identityId()).isEqualTo(IDENTITY_ID);
        assertThat(challenge.challengeDigest())
                .isEqualTo("stored-challenge-digest")
                .doesNotContain("raw-challenge-token");
        assertThat(challenge.status())
                .isEqualTo(AuthenticationChallengeStatus.PENDING);
        assertThat(challenge.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(challenge.createdAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsUnknownKsNumberWithoutCreatingChallenge() {
        AtomicReference<AuthenticationChallenge> inserted =
                new AtomicReference<>();

        DefaultAuthenticationService service = service(
                Optional.empty(),
                Optional.of(activeIdentity()),
                (raw, hash) -> true,
                inserted);

        assertInvalidCredentials(service);
        assertThat(inserted.get()).isNull();
    }

    @Test
    void rejectsInactiveCredentialWithoutCreatingChallenge() {
        AtomicReference<AuthenticationChallenge> inserted =
                new AtomicReference<>();

        AuthenticationCredential inactive =
                new AuthenticationCredential(
                        IDENTITY_ID.toString(),
                        "KS000001",
                        "James",
                        "stored-hash",
                        false);

        DefaultAuthenticationService service = service(
                Optional.of(inactive),
                Optional.of(activeIdentity()),
                (raw, hash) -> true,
                inserted);

        assertInvalidCredentials(service);
        assertThat(inserted.get()).isNull();
    }

    @Test
    void rejectsIncorrectPasswordWithoutCreatingChallenge() {
        AtomicReference<AuthenticationChallenge> inserted =
                new AtomicReference<>();

        DefaultAuthenticationService service = service(
                Optional.of(activeCredential()),
                Optional.of(activeIdentity()),
                (raw, hash) -> false,
                inserted);

        assertInvalidCredentials(service);
        assertThat(inserted.get()).isNull();
    }

    @Test
    void rejectsInactiveIdentityWithoutCreatingChallenge() {
        AtomicReference<AuthenticationChallenge> inserted =
                new AtomicReference<>();

        DefaultAuthenticationService service = service(
                Optional.of(activeCredential()),
                Optional.of(identity(IdentityStatus.SUSPENDED)),
                (raw, hash) -> true,
                inserted);

        assertInvalidCredentials(service);
        assertThat(inserted.get()).isNull();
    }

    @Test
    void rejectsNonCanonicalKsNumberWithoutCreatingChallenge() {
        AtomicReference<AuthenticationChallenge> inserted =
                new AtomicReference<>();

        DefaultAuthenticationService service = service(
                Optional.of(activeCredential()),
                Optional.of(activeIdentity()),
                (raw, hash) -> true,
                inserted);

        AuthenticateCommand nonCanonical = new AuthenticateCommand(
                "ks000001",
                "correct-password",
                "securepay-web",
                "device-1",
                "ip-hash-1");

        assertThatThrownBy(() -> service.authenticate(nonCanonical))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
        assertThat(inserted.get()).isNull();
    }

    private DefaultAuthenticationService service(
            Optional<AuthenticationCredential> credential,
            Optional<KsIdentityRecord> identity,
            PasswordVerifier verifier,
            AtomicReference<AuthenticationChallenge> inserted) {
        AuthenticationCredentialRepository credentialRepository =
                ksNumber -> credential;

        KsIdentityQueryService identityQueryService =
                new KsIdentityQueryService() {
                    @Override
                    public Optional<KsIdentityRecord> findById(UUID identityId) {
                        return identity;
                    }

                    @Override
                    public Optional<KsIdentityRecord> findByCanonicalKsNumber(
                            String canonicalKsNumber) {
                        return identity;
                    }

                    @Override
                    public Optional<KsIdentityRecord> findByNormalizedAlias(
                            String normalizedAlias) {
                        return Optional.empty();
                    }
                };

        AuthenticationChallengeRepository challengeRepository =
                new AuthenticationChallengeRepository() {
                    @Override
                    public void insert(AuthenticationChallenge challenge) {
                        inserted.set(challenge);
                    }

                    @Override
                    public Optional<AuthenticationChallenge>
                            findByChallengeDigest(String challengeDigest) {
                        return Optional.empty();
                    }

                    @Override
                    public boolean consumePendingChallenge(
                            AuthenticationChallenge challenge) {
                        return false;
                    }
                };

        return new DefaultAuthenticationService(
                credentialRepository,
                identityQueryService,
                challengeRepository,
                verifier,
                () -> "raw-challenge-token",
                token -> "stored-challenge-digest",
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthenticationCredential activeCredential() {
        return new AuthenticationCredential(
                IDENTITY_ID.toString(),
                "KS000001",
                "James",
                "stored-hash",
                true);
    }

    private KsIdentityRecord activeIdentity() {
        return identity(IdentityStatus.ACTIVE);
    }

    private KsIdentityRecord identity(IdentityStatus status) {
        return new KsIdentityRecord(
                IDENTITY_ID,
                KsNumber.fromSequence(1L),
                1L,
                IdentityType.INDIVIDUAL,
                status,
                "James",
                "request-001",
                "request-hash-001",
                "SYSTEM",
                "securepay-core",
                "request-id-001",
                "correlation-id-001",
                NOW,
                NOW,
                status == IdentityStatus.SUSPENDED ? NOW : null,
                null,
                0L);
    }

    private void assertInvalidCredentials(
            DefaultAuthenticationService service) {
        assertThatThrownBy(() -> service.authenticate(COMMAND))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }
}
