package ke.securepay.core.api.identity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.exception.IssuanceOwnershipConflictException;
import ke.securepay.platform.identity.ksnumber.KsNumber;
import ke.securepay.platform.identity.model.IdentityStatus;
import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.identity.result.IssuedKsIdentityResult;
import ke.securepay.platform.identity.service.KsIdentityIssuanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IdentityController.class)
class IdentityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KsIdentityIssuanceService issuanceService;

    @Test
    void issuesIdentityAndReturnsCreatedResponse() throws Exception {
        UUID identityId =
                UUID.fromString("11111111-1111-1111-1111-111111111111");

        IssuedKsIdentityResult result =
                new IssuedKsIdentityResult(
                        identityId,
                        KsNumber.fromSequence(1L),
                        1L,
                        IdentityType.INDIVIDUAL,
                        IdentityStatus.ACTIVE,
                        false
                );

        when(issuanceService.issue(any(IssueKsIdentityCommand.class)))
                .thenReturn(result);

        mockMvc.perform(
                        post("/api/v1/identities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "issuanceRequestKey": "request-001",
                                          "identityType": "INDIVIDUAL",
                                          "displayName": "James Kimani"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identityId")
                        .value(identityId.toString()))
                .andExpect(jsonPath("$.canonicalKsNumber")
                        .value("KS001"))
                .andExpect(jsonPath("$.sequenceNumber")
                        .value(1))
                .andExpect(jsonPath("$.identityType")
                        .value("INDIVIDUAL"))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"))
                .andExpect(jsonPath("$.replayed")
                        .value(false));

        verify(issuanceService)
                .issue(any(IssueKsIdentityCommand.class));
    }
    @Test
    void rejectsInvalidRequestWithValidationErrorResponse() throws Exception {
        mockMvc.perform(
                        post("/api/v1/identities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "issuanceRequestKey": "",
                                          "identityType": null,
                                          "displayName": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.issuanceRequestKey")
                        .value("must not be blank"))
                .andExpect(jsonPath("$.fieldErrors.identityType")
                        .value("must not be null"))
                .andExpect(jsonPath("$.fieldErrors.displayName")
                        .value("must not be blank"));

        verify(issuanceService, never())
                .issue(any(IssueKsIdentityCommand.class));
    }


    @Test
    void returnsConflictWhenIssuanceRequestKeyHasDifferentOwnership() throws Exception {
        when(issuanceService.issue(any(IssueKsIdentityCommand.class)))
                .thenThrow(new IssuanceOwnershipConflictException(
                        "Issuance request key already owns an identity with a different request fingerprint"));

        mockMvc.perform(
                        post("/api/v1/identities")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "issuanceRequestKey": "request-001",
                                          "identityType": "INDIVIDUAL",
                                          "displayName": "Different Person"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("ISSUANCE_OWNERSHIP_CONFLICT"))
                .andExpect(jsonPath("$.message")
                        .value("Issuance request key already owns an identity with a different request fingerprint"));
    }
}
