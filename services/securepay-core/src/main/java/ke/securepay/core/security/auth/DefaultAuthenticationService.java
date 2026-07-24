package ke.securepay.core.security.auth;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import ke.securepay.core.security.auth.challenge.AuthenticationChallenge;
import ke.securepay.core.security.auth.challenge.AuthenticationChallengeRepository;
import ke.securepay.core.security.auth.challenge.AuthenticationChallengeStatus;
import ke.securepay.core.security.auth.credential.AuthenticationCredential;
import ke.securepay.core.security.auth.credential.AuthenticationCredentialRepository;
import ke.securepay.core.security.password.PasswordVerifier;
import ke.securepay.core.security.token.OpaqueTokenGenerator;
import ke.securepay.core.security.token.TokenDigester;
import ke.securepay.platform.identity.ksnumber.InvalidKsNumberException;
import ke.securepay.platform.identity.ksnumber.KsNumber;
import ke.securepay.platform.identity.model.IdentityStatus;
import ke.securepay.platform.identity.service.KsIdentityQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default primary-credential authentication implementation.
 *
 * Successful password verification creates a persistent pending MFA challenge.
 * No active session is created by this service.
 */
@Service
public final class DefaultAuthenticationService
        implements AuthenticationService {

    private static final String AUTHENTICATION_METHOD = "PASSWORD";
    private static final Duration CHALLENGE_LIFETIME =
            Duration.ofMinutes(5);

    private final AuthenticationCredentialRepository credentialRepository;
    private final KsIdentityQueryService identityQueryService;
    private final AuthenticationChallengeRepository challengeRepository;
    private final PasswordVerifier passwordVerifier;
    private final OpaqueTokenGenerator tokenGenerator;
    private final TokenDigester tokenDigester;
    private final Clock clock;

    public DefaultAuthenticationService(
            AuthenticationCredentialRepository credentialRepository,
            KsIdentityQueryService identityQueryService,
            AuthenticationChallengeRepository challengeRepository,
            PasswordVerifier passwordVerifier,
            OpaqueTokenGenerator tokenGenerator,
            TokenDigester tokenDigester,
            Clock clock) {
        this.credentialRepository =
                Objects.requireNonNull(
                        credentialRepository,
                        "credentialRepository");
        this.identityQueryService =
                Objects.requireNonNull(
                        identityQueryService,
                        "identityQueryService");
        this.challengeRepository =
                Objects.requireNonNull(
                        challengeRepository,
                        "challengeRepository");
        this.passwordVerifier =
                Objects.requireNonNull(
                        passwordVerifier,
                        "passwordVerifier");
        this.tokenGenerator =
                Objects.requireNonNull(
                        tokenGenerator,
                        "tokenGenerator");
        this.tokenDigester =
                Objects.requireNonNull(
                        tokenDigester,
                        "tokenDigester");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public PendingAuthenticationResult authenticate(
            AuthenticateCommand command) {
        Objects.requireNonNull(command, "command");

        String canonicalKsNumber = parseCanonicalKsNumber(
                command.ksNumber());

        AuthenticationCredential credential = credentialRepository
                .findByKsNumber(canonicalKsNumber)
                .filter(AuthenticationCredential::active)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordVerifier.matches(
                command.password(),
                credential.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        UUID identityId = identityQueryService
                .findByCanonicalKsNumber(canonicalKsNumber)
                .filter(identity -> identity.status() == IdentityStatus.ACTIVE)
                .map(identity -> identity.id())
                .orElseThrow(InvalidCredentialsException::new);

        if (!identityId.toString().equals(credential.actorId())) {
            throw new InvalidCredentialsException();
        }

        var now = clock.instant();
        var expiresAt = now.plus(CHALLENGE_LIFETIME);
        var challengeId = UUID.randomUUID();
        String challengeToken = tokenGenerator.generate();

        challengeRepository.insert(
                new AuthenticationChallenge(
                        challengeId,
                        identityId,
                        tokenDigester.digest(challengeToken),
                        AuthenticationChallengeStatus.PENDING,
                        command.applicationId(),
                        command.deviceId(),
                        command.sourceIpHash(),
                        expiresAt,
                        null,
                        null,
                        now,
                        0L));

        AuthenticationPrincipal principal =
                new AuthenticationPrincipal(
                        credential.actorId(),
                        credential.ksNumber(),
                        credential.displayName(),
                        AUTHENTICATION_METHOD,
                        command.applicationId(),
                        command.deviceId(),
                        command.sourceIpHash());

        return new PendingAuthenticationResult(
                challengeId,
                challengeToken,
                expiresAt,
                principal);
    }

    private String parseCanonicalKsNumber(String rawKsNumber) {
        try {
            return KsNumber.parse(rawKsNumber).canonicalValue();
        } catch (InvalidKsNumberException exception) {
            throw new InvalidCredentialsException();
        }
    }
}
