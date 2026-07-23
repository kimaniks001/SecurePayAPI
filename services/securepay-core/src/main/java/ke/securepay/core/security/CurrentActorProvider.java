package ke.securepay.core.security;

import ke.securepay.platform.persistence.actor.ActorContext;

/**
 * Supplies the trusted actor for the current request.
 *
 * Controllers must depend on this boundary instead of constructing
 * system actors directly.
 */
public interface CurrentActorProvider {

    ActorContext currentActor();
}
