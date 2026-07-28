package ca.refundr.highspring.api.resource.version1.admin;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.scope.RequestScope;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Admin-only tools under {@code /v1/admin/}.
 *
 * <p>Every child should call {@code requireAdmin()} so customers get HTTP 403.
 *
 * <pre>
 *   /v1/admin/totals/      sales KPIs
 *   /v1/admin/purchases/   recent orders
 *   /v1/admin/errors/      saved 500 stack traces (list + delete)
 *   /v1/admin/allure/      published Allure HTML report
 *   /v1/admin/javadoc/     published JavaDoc + HTTP status guide
 *   /v1/admin/boom/        demo 500 (only if ENABLE_BOOM_ENDPOINT=true)
 * </pre>
 *
 * <p>This is another “route table” node: add new admin pages by appending to
 * {@link #getDescendantByPath(String)}.
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
		children.add(() -> new AdminJavadocResource(scope, this));
		if ("true".equalsIgnoreCase(scope.getConfiguration().getString("ENABLE_BOOM_ENDPOINT", "false"))) {
			children.add(() -> new AdminBoomResource(scope, this));
		}
		return getDescendantFromChildren(relativePath, children);
	}
}
