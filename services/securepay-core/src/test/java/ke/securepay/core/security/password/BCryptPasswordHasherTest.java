package ke.securepay.core.security.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher passwordHasher =
            new BCryptPasswordHasher();

    @Test
    void hashesAndVerifiesPassword() {
        String passwordHash = passwordHasher.hash("StrongPassword123!");

        assertThat(passwordHash).isNotBlank();
        assertThat(passwordHash).isNotEqualTo("StrongPassword123!");
        assertThat(passwordHasher.matches(
                "StrongPassword123!",
                passwordHash))
                .isTrue();
    }

    @Test
    void rejectsIncorrectPassword() {
        String passwordHash = passwordHasher.hash("StrongPassword123!");

        assertThat(passwordHasher.matches(
                "WrongPassword123!",
                passwordHash))
                .isFalse();
    }

    @Test
    void createsDifferentHashesForSamePassword() {
        String firstHash = passwordHasher.hash("StrongPassword123!");
        String secondHash = passwordHasher.hash("StrongPassword123!");

        assertThat(firstHash).isNotEqualTo(secondHash);
        assertThat(passwordHasher.matches(
                "StrongPassword123!",
                firstHash))
                .isTrue();
        assertThat(passwordHasher.matches(
                "StrongPassword123!",
                secondHash))
                .isTrue();
    }

    @Test
    void rejectsNullInputs() {
        assertThatNullPointerException()
                .isThrownBy(() -> passwordHasher.hash(null))
                .withMessage("rawPassword");

        assertThatNullPointerException()
                .isThrownBy(() -> passwordHasher.matches(
                        null,
                        "$2a$10$invalid"))
                .withMessage("rawPassword");

        assertThatNullPointerException()
                .isThrownBy(() -> passwordHasher.matches(
                        "password",
                        null))
                .withMessage("passwordHash");
    }
}
