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
 * Version 1 of the public API — all shopping cart endpoints live under /v1/.
 */
public final class Version1Resource extends AbstractChildResource<RootResource> {

	public static final int VERSION_1 = 1;

	public Version1Resource(RequestScope scope, RootResource parent) {
		super(scope, parent);
	}

	@Override
	public String getRelativePath() {
		return "v1/";
	}

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
