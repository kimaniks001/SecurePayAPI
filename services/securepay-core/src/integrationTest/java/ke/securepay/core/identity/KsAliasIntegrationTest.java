package ke.securepay.core.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import ke.securepay.core.support.SecurePayIntegrationTest;
import ke.securepay.platform.identity.alias.AliasNormalizer;
import ke.securepay.platform.identity.command.AliasLifecycleTransitionCommand;
import ke.securepay.platform.identity.command.CreateAliasCommand;
import ke.securepay.platform.identity.command.IssueKsIdentityCommand;
import ke.securepay.platform.identity.events.IdentityEventTypes;
import ke.securepay.platform.identity.model.AliasStatus;
import ke.securepay.platform.identity.model.AliasType;
import ke.securepay.platform.identity.model.IdentityType;
import ke.securepay.platform.identity.service.KsAliasService;
import ke.securepay.platform.identity.service.KsIdentityIssuanceService;
import ke.securepay.platform.identity.service.KsIdentityQueryService;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import ke.securepay.platform.persistence.exception.OptimisticLockException;
import ke.securepay.platform.testing.support.DockerAssumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

@SecurePayIntegrationTest
class KsAliasIntegrationTest {

    @Autowired
    private KsIdentityIssuanceService issuanceService;

    @Autowired
    private KsAliasService aliasService;

    @Autowired
    private KsIdentityQueryService queryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void enforceDockerPolicy() {
        DockerAssumptions.enforceDockerPolicyForIntegrationTests();
    }

    @Test
    void aliasCreationAndLookupWorkWithoutConsumingSequence() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "alias-base-" + suffix, IdentityType.TEST, "Alias Base", actor));

        Long sequenceBefore = jdbcTemplate.queryForObject(
                "SELECT last_value FROM identity.ks_number_sequence", Long.class);

        var alias = aliasService.createAlias(new CreateAliasCommand(
                issued.identityId(), "my.alias." + suffix, AliasType.MEMORABLE, true, actor));

        Long sequenceAfter = jdbcTemplate.queryForObject(
                "SELECT last_value FROM identity.ks_number_sequence", Long.class);
        assertThat(sequenceAfter).isEqualTo(sequenceBefore);

        var resolved = queryService.findByNormalizedAlias(alias.normalizedAlias()).orElseThrow();
        assertThat(resolved.canonicalKsNumber()).isEqualTo(issued.canonicalKsNumber());
        assertThat(resolved.canonicalKsNumber().canonicalValue())
                .isEqualTo(issued.canonicalKsNumber().canonicalValue());
    }

    @Test
    void duplicateNormalizedAliasIsRejected() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "alias-dup-" + suffix, IdentityType.TEST, "Dup", actor));
        String aliasValue = "dup.alias." + suffix;

        aliasService.createAlias(new CreateAliasCommand(
                issued.identityId(), aliasValue, AliasType.MEMORABLE, false, actor));

        var other = issuanceService.issue(new IssueKsIdentityCommand(
                "alias-dup-other-" + suffix, IdentityType.TEST, "Other", actor));

        assertThatThrownBy(() -> aliasService.createAlias(new CreateAliasCommand(
                        other.identityId(), aliasValue.toUpperCase(), AliasType.MEMORABLE, false, actor)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void canonicalLookingAliasRejectedByNormalizer() {
        assertThatThrownBy(() -> AliasNormalizer.normalizeOrThrow("KS123"))
                .isInstanceOf(ke.securepay.platform.identity.alias.InvalidAliasException.class);
    }

    @Test
    void retiredAliasRemainsReservedAndCreatesAuditOutbox() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "alias-retire-" + suffix, IdentityType.TEST, "Retire", actor));
        var alias = aliasService.createAlias(new CreateAliasCommand(
                issued.identityId(), "retire." + suffix, AliasType.MEMORABLE, false, actor));

        aliasService.transitionAlias(new AliasLifecycleTransitionCommand(
                alias.id(), AliasStatus.ACTIVE, "activate", actor));
        aliasService.transitionAlias(new AliasLifecycleTransitionCommand(
                alias.id(), AliasStatus.RETIRED, "retire", actor));

        assertThatThrownBy(() -> aliasService.createAlias(new CreateAliasCommand(
                        issued.identityId(), "retire." + suffix, AliasType.MEMORABLE, false, actor)))
                .isInstanceOf(DuplicateKeyException.class);

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit.audit_events WHERE event_type = ?",
                Integer.class,
                IdentityEventTypes.ALIAS_STATUS_CHANGED);
        assertThat(auditCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void aliasLifecycleOptimisticLockConflictFails() {
        String suffix = UUID.randomUUID().toString();
        var actor = ActorContextFactory.test("securepay-core");
        var issued = issuanceService.issue(new IssueKsIdentityCommand(
                "alias-lock-" + suffix, IdentityType.TEST, "Lock", actor));
        var alias = aliasService.createAlias(new CreateAliasCommand(
                issued.identityId(), "lock." + suffix, AliasType.MEMORABLE, false, actor));

        jdbcTemplate.update(
                "UPDATE identity.ks_number_aliases SET version = version + 1 WHERE id = ?", alias.id());

        assertThatThrownBy(() -> aliasService.transitionAlias(new AliasLifecycleTransitionCommand(
                        alias.id(), AliasStatus.ACTIVE, "activate", actor)))
                .isInstanceOf(OptimisticLockException.class);
    }
}
