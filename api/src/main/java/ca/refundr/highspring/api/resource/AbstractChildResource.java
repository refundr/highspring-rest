package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.scope.RequestScope;
import com.google.common.base.Preconditions;

/**
 * A resource that hangs under a parent path (for example /v1/products under /v1/).
 */
public abstract class AbstractChildResource<P extends AbstractResource> extends AbstractResource {

	protected final P parent;

	protected AbstractChildResource(RequestScope scope, P parent) {
		super(scope);
		this.parent = Preconditions.checkNotNull(parent, "parent");
	}
}
