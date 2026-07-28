package ca.refundr.highspring.api.resource.version1;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.RootResource;
import ca.refundr.highspring.api.resource.version1.admin.AdminResource;
import ca.refundr.highspring.api.resource.version1.auth.AuthResource;
import ca.refundr.highspring.api.resource.version1.cart.CartResource;
import ca.refundr.highspring.api.resource.version1.me.MeResource;
import ca.refundr.highspring.api.resource.version1.product.ProductsResource;
import ca.refundr.highspring.api.resource.version1.purchase.PurchasesResource;
import ca.refundr.highspring.api.scope.RequestScope;

import java.util.List;

/**
 * Version 1 of the public API — everything under {@code /v1/}.
 *
 * <h2>Role in the HTTP chain</h2>
 *
 * <p>{@link RootResource} owns {@code /}. This class owns the next segment {@code v1/}.
 * When the filter asks for {@code v1/products/}:
 * <ol>
 *   <li>Root matches and strips nothing special (root path is {@code ""}).</li>
 *   <li>Root’s child list finds this {@code Version1Resource} because the path starts with {@code v1/}.</li>
 *   <li>This class strips {@code v1/} and looks at the remainder {@code products/}.</li>
 *   <li>{@link #getDescendantByPath(String)} tries each registered child until one matches.</li>
 * </ol>
 *
 * <h2>Map of /v1 children (memorize this)</h2>
 *
 * <pre>
 *   /v1/auth/...        {@link AuthResource}       Google login (url + callback)
 *   /v1/me/             {@link MeResource}         current session user
 *   /v1/products/       {@link ProductsResource}   catalog (read-only)
 *   /v1/cart/...        {@link CartResource}       persisted cart + checkout
 *   /v1/purchases/...   {@link PurchasesResource}  create/fetch purchases
 *   /v1/admin/...       {@link AdminResource}      ADMIN-only tools
 * </pre>
 *
 * <h2>How to add a new endpoint</h2>
 *
 * <ol>
 *   <li>Create a class extending {@link AbstractChildResource}{@code <Version1Resource>}
 *       (or nest under an existing child like {@code AuthResource}).</li>
 *   <li>Implement {@code getRelativePath()} — must end with {@code /} (e.g. {@code "wishlist/"}).</li>
 *   <li>Register it in {@link #getDescendantByPath(String)} below.</li>
 *   <li>Override {@code httpGet}/{@code httpPost}/… and add those methods to {@code supportedMethods}
 *       in the constructor.</li>
 * </ol>
 *
 * <p>There is no central “routes file”. <strong>This method is the route table for v1.</strong>
 *
 * @see RootResource
 * @see ca.refundr.highspring.api.resource.package-info
 */
public final class Version1Resource extends AbstractChildResource<RootResource> {

	/** Documented API major version number (informational). */
	public static final int VERSION_1 = 1;

	public Version1Resource(RequestScope scope, RootResource parent) {
		super(scope, parent);
	}

	/**
	 * URL segment this node owns. Trailing slash keeps path matching consistent across the tree.
	 */
	@Override
	public String getRelativePath() {
		return "v1/";
	}

	/**
	 * Route table for everything under {@code /v1/}.
	 *
	 * <p>Order rarely matters unless two children claim the same prefix (avoid that).
	 * Each supplier builds a candidate; {@code getDescendantFromChildren} returns the first match.
	 *
	 * @param relativePath path <em>after</em> {@code v1/}, e.g. {@code "cart/items/"} or {@code "products/"}
	 */
	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return getDescendantFromChildren(relativePath, List.of(
			() -> new AuthResource(scope, this),
			() -> new MeResource(scope, this),
			() -> new ProductsResource(scope, this),
			() -> new CartResource(scope, this),
			() -> new PurchasesResource(scope, this),
			() -> new AdminResource(scope, this)
		));
	}
}
