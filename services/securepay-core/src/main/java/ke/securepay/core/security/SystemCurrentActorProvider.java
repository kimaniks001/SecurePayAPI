package ke.securepay.core.security;

import ke.securepay.platform.persistence.actor.ActorContext;
import ke.securepay.platform.persistence.actor.ActorContextFactory;
import org.springframework.stereotype.Component;

/**
 * Transitional actor provider used until request authentication
 * supplies a verified KSNumber user actor.
 */
@Component
public final class SystemCurrentActorProvider implements CurrentActorProvider {

    private static final String SOURCE_SERVICE = "securepay-core";

    @Override
    public ActorContext currentActor() {
        return ActorContextFactory.system(SOURCE_SERVICE);
    }
}
