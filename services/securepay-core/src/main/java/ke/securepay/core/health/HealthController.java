package ke.securepay.core.health;

import java.util.List;
import ke.securepay.platform.common.errors.FoundationErrorCode;
import ke.securepay.platform.web.api.ErrorBody;
import ke.securepay.platform.web.api.ErrorEnvelope;
import ke.securepay.platform.web.api.ResponseMetaFactory;
import ke.securepay.platform.web.api.SuccessEnvelope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final DependencyHealthService dependencyHealthService;
    private final ResponseMetaFactory responseMetaFactory;

    public HealthController(
            DependencyHealthService dependencyHealthService, ResponseMetaFactory responseMetaFactory) {
        this.dependencyHealthService = dependencyHealthService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/live")
    public SuccessEnvelope<LiveHealthData> live() {
        return SuccessEnvelope.of(LiveHealthData.alive(), responseMetaFactory.current());
    }

    @GetMapping("/ready")
    public ResponseEntity<?> ready() {
        if (dependencyHealthService.isReady()) {
            return ResponseEntity.ok(SuccessEnvelope.of(ReadyHealthData.ready(), responseMetaFactory.current()));
        }
        ErrorEnvelope error = ErrorEnvelope.of(
                new ErrorBody(
                        FoundationErrorCode.INTERNAL_ERROR.name(),
                        "Service is not ready to receive traffic.",
                        null),
                responseMetaFactory.current());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @GetMapping("/dependencies")
    public ResponseEntity<SuccessEnvelope<DependenciesDataResponse>> dependencies() {
        List<DependencyHealthResponse> dependencies = dependencyHealthService.checkAll();
        SuccessEnvelope<DependenciesDataResponse> body = SuccessEnvelope.of(
                new DependenciesDataResponse(dependencies), responseMetaFactory.current());
        if (dependencyHealthService.hasUnavailableDependency()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
        return ResponseEntity.ok(body);
    }
}
