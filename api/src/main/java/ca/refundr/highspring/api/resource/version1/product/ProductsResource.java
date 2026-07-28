package ca.refundr.highspring.api.resource.version1.product;

import ca.refundr.highspring.api.resource.AbstractChildResource;
import ca.refundr.highspring.api.resource.AbstractResource;
import ca.refundr.highspring.api.resource.version1.Version1Resource;
import ca.refundr.highspring.api.scope.RequestScope;
import ca.refundr.highspring.api.util.ServerResponse;
import ca.refundr.highspring.database.row.ProductRow;
import ca.refundr.highspring.domain.product.ProductResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.jooq.impl.DSL;

import java.util.List;

import static org.eclipse.jetty.http.HttpStatus.METHOD_NOT_ALLOWED_405;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

/**
 * Lists products from the catalog (read-only — products are seeded in the database).
 */
public final class ProductsResource extends AbstractChildResource<Version1Resource> {

	public ProductsResource(RequestScope scope, Version1Resource parent) {
		super(scope, parent);
		supportedMethods.add(HttpMethod.GET.asString());
	}

	@Override
	public String getRelativePath() {
		return "products/";
	}

	@Override
	public ServerResponse httpGet() {
		if (!scope.getRequest().acceptsJson()) {
			return writer -> writer.sendEmpty(METHOD_NOT_ALLOWED_405);
		}
		requireSessionUser();
		List<ProductResponse> products = scope.getDatabase().transactionWithResult(connection -> {
			List<ProductRow> rows = ProductRow.listActive(DSL.using(connection));
			return rows.stream()
				.map(row -> new ProductResponse(
					row.getId(),
					row.getName(),
					row.getUnitPrice(),
					row.getCategoryId(),
					row.getCategoryCode(),
					row.getCategoryName(),
					row.getDiscountPercent()
				))
				.toList();
		});
		return writer -> writer.sendJson(OK_200, products);
	}

	@Override
	protected <T extends AbstractResource> T getDescendantByPath(String relativePath) {
		return null;
	}
}
