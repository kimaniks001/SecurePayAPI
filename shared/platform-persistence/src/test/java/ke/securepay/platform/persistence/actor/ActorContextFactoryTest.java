package ke.securepay.platform.persistence.actor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ActorContextFactoryTest {

    @Test
    void createsAuthenticatedUserActor() {
        ActorContext actor = ActorContextFactory.user(
                "user-123",
                "KS001",
                "password_otp",
                "securepay-core",
                "securepay-web",
                "ip-hash-123",
                "device-123"
        );

        assertThat(actor.actorType()).isEqualTo(ActorType.USER);
        assertThat(actor.actorId()).isEqualTo("user-123");
        assertThat(actor.actorKsNumber()).isEqualTo("KS001");
        assertThat(actor.applicationId()).isEqualTo("securepay-web");
        assertThat(actor.authenticated()).isTrue();
        assertThat(actor.authenticationMethod()).isEqualTo("password_otp");
        assertThat(actor.sourceService()).isEqualTo("securepay-core");
        assertThat(actor.sourceIpHash()).isEqualTo("ip-hash-123");
        assertThat(actor.deviceId()).isEqualTo("device-123");
        assertThat(actor.requestId()).isNotBlank();
        assertThat(actor.correlationId()).isNotBlank();
    }
}
