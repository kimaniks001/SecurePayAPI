package ke.securepay.core.health;

import java.util.List;
import ke.securepay.core.startup.ApplicationStartupState;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DependencyHealthService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationStartupState applicationStartupState;

    public DependencyHealthService(
            JdbcTemplate jdbcTemplate,
            StringRedisTemplate redisTemplate,
            ApplicationStartupState applicationStartupState) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.applicationStartupState = applicationStartupState;
    }

    public List<DependencyHealthResponse> checkAll() {
        return List.of(checkPostgres(), checkRedis(), checkApplication());
    }

    public boolean isReady() {
        return checkAll().stream().noneMatch(dependency -> DependencyStatus.UNAVAILABLE.apiValue().equals(dependency.status()));
    }

    public boolean hasUnavailableDependency() {
        return checkAll().stream().anyMatch(dependency -> DependencyStatus.UNAVAILABLE.apiValue().equals(dependency.status()));
    }

    private DependencyHealthResponse checkPostgres() {
        long startedAt = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return DependencyHealthResponse.of(
                    "postgres",
                    DependencyStatus.HEALTHY,
                    System.currentTimeMillis() - startedAt,
                    "PostgreSQL is reachable.");
        } catch (DataAccessException ex) {
            return DependencyHealthResponse.of(
                    "postgres",
                    DependencyStatus.UNAVAILABLE,
                    System.currentTimeMillis() - startedAt,
                    "PostgreSQL is unavailable.");
        }
    }

    private DependencyHealthResponse checkRedis() {
        long startedAt = System.currentTimeMillis();
        try {
            String pong = redisTemplate.execute((RedisConnection connection) -> connection.ping());
            DependencyStatus status = "PONG".equalsIgnoreCase(pong) ? DependencyStatus.HEALTHY : DependencyStatus.DEGRADED;
            return DependencyHealthResponse.of(
                    "redis",
                    status,
                    System.currentTimeMillis() - startedAt,
                    status == DependencyStatus.HEALTHY ? "Redis is reachable." : "Redis responded unexpectedly.");
        } catch (RuntimeException ex) {
            return DependencyHealthResponse.of(
                    "redis",
                    DependencyStatus.UNAVAILABLE,
                    System.currentTimeMillis() - startedAt,
                    "Redis is unavailable.");
        }
    }

    private DependencyHealthResponse checkApplication() {
        long startedAt = System.currentTimeMillis();
        if (applicationStartupState.isReady()) {
            return DependencyHealthResponse.of(
                    "application",
                    DependencyStatus.HEALTHY,
                    System.currentTimeMillis() - startedAt,
                    "Application startup completed.");
        }
        return DependencyHealthResponse.of(
                "application",
                DependencyStatus.DEGRADED,
                System.currentTimeMillis() - startedAt,
                "Application startup is still in progress.");
    }
}
