package ke.securepay.platform.web.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaginationMeta(
        @JsonProperty("page") Integer page,
        @JsonProperty("page_size") Integer pageSize,
        @JsonProperty("total_items") Long totalItems,
        @JsonProperty("total_pages") Integer totalPages,
        @JsonProperty("has_next") Boolean hasNext,
        @JsonProperty("has_previous") Boolean hasPrevious,
        @JsonProperty("next_cursor") String nextCursor,
        @JsonProperty("previous_cursor") String previousCursor) {}
