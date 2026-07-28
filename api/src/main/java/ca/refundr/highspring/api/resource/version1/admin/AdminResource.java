package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.scope.RequestScope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Admin-only tools: sales totals, purchase history, error log, and Allure test report.
 */
public final class AdminResource extends AbstractChildResource<Version1Resource> {

	public AdminResource(RequestScope scope, Version1Resource parent) {
		super(scope, parent);
	}

	@Override
	public String getRelativePath() {
		return "admin/";
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		List<Supplier<? extends AbstractResource>> children = new ArrayList<>();
		children.add(() -> new AdminTotalsResource(scope, this));
		children.add(() -> new AdminPurchasesResource(scope, this));
		children.add(() -> new AdminErrorsResource(scope, this));
		children.add(() -> new AdminAllureResource(scope, this));
		if ("true".equalsIgnoreCase(scope.getConfiguration().getString("ENABLE_BOOM_ENDPOINT", "false"))) {
			children.add(() -> new AdminBoomResource(scope, this));
		}
		return getDescendantFromChildren(relativePath, children);
	}
}
