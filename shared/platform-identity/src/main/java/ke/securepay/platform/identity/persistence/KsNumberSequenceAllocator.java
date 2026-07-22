package ke.securepay.platform.identity.persistence;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class KsNumberSequenceAllocator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public KsNumberSequenceAllocator(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long allocateNext() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT nextval('identity.ks_number_sequence')", java.util.Map.of(), Long.class);
        if (value == null) {
            throw new IllegalStateException("Sequence allocation returned null");
        }
        return value;
    }
}
