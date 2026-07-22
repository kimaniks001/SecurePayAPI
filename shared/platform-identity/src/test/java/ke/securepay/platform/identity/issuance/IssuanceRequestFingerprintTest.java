package ke.securepay.platform.identity.issuance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IssuanceRequestFingerprintTest {

    @Test
    void sameBodyProducesSameHash() {
        String body = "{\"issuance_request_key\":\"k1\",\"identity_type\":\"TEST\",\"display_name\":\"A\"}";
        assertThat(IssuanceRequestFingerprint.hash(body)).isEqualTo(IssuanceRequestFingerprint.hash(body));
    }

    @Test
    void differentBodyProducesDifferentHash() {
        String a = "{\"issuance_request_key\":\"k1\",\"identity_type\":\"TEST\",\"display_name\":\"A\"}";
        String b = "{\"issuance_request_key\":\"k1\",\"identity_type\":\"TEST\",\"display_name\":\"B\"}";
        assertThat(IssuanceRequestFingerprint.hash(a)).isNotEqualTo(IssuanceRequestFingerprint.hash(b));
    }
}
