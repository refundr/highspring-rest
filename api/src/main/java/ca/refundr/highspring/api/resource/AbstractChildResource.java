package ca.refundr.highspring.api.resource;

import ca.refundr.highspring.api.scope.RequestScope;
import com.google.common.base.Preconditions;

/**
 * A resource that lives under a parent path.
 *
 * A child resource can call shared functionality on its parent (via the parent field).
 *
 * <p>Example: {@code ProductsResource} is a child of {@code Version1Resource}, so the full URL is
 * {@code /} + {@code v1/} + {@code products/} → {@code /v1/products/}.
 *
 * <p>Keeping a typed {@link #parent} pointer makes the tree easy to navigate in the debugger and
 * documents which API section you are in (auth under auth, admin under admin, …).
 *
 *
 *
 * @param <P> parent resource type (for clarity when reading call sites)
 * @see AbstractResource
 * @see RootResource
 */
public abstract class AbstractChildResource<P extends AbstractResource> extends AbstractResource {

	/** Immediate parent in the URL tree (never null). */
	protected final P parent;

	protected AbstractChildResource(RequestScope scope, P parent) {
		super(scope);
		this.parent = Preconditions.checkNotNull(parent, "parent");
	}
}
