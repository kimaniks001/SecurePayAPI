package ke.securepay.core.security.auth.challenge;

import java.util.Optional;

public interface AuthenticationChallengeRepository {

    void insert(AuthenticationChallenge challenge);

    Optional<AuthenticationChallenge> findByChallengeDigest(String challengeDigest);

    boolean consumePendingChallenge(
            AuthenticationChallenge challenge);
}
