package ke.securepay.core.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DependenciesDataResponse(@JsonProperty("dependencies") List<DependencyHealthResponse> dependencies) {}
