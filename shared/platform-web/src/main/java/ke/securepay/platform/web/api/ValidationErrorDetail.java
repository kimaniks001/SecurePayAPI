package ke.securepay.platform.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorDetail(
        @JsonProperty("field") String field,
        @JsonProperty("issue") String issue,
        @JsonProperty("code") String code) {}
