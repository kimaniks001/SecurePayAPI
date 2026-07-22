package ke.securepay.platform.persistence.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import ke.securepay.platform.observability.metrics.PersistenceMetricNames;
import ke.securepay.platform.persistence.outbox.OutboxRepository;
import ke.securepay.platform.persistence.outbox.OutboxStatus;
import org.springframework.stereotype.Component;

@Component
public class PersistenceMetricsRegistrar {

    private final MeterRegistry meterRegistry;
    private final OutboxRepository outboxRepository;

    public PersistenceMetricsRegistrar(MeterRegistry meterRegistry, OutboxRepository outboxRepository) {
        this.meterRegistry = meterRegistry;
        this.outboxRepository = outboxRepository;
    }

    @PostConstruct
    void registerGauges() {
        Gauge.builder(PersistenceMetricNames.OUTBOX_PENDING, outboxRepository, repo -> repo.countByStatus(OutboxStatus.PENDING))
                .register(meterRegistry);
    }
}
