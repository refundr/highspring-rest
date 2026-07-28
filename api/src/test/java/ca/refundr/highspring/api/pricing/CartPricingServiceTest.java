package ca.refundr.highspring.api.pricing;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Epic("Shopping cart pricing")
@Feature("CartPricingService")
public class CartPricingServiceTest {

	private final CartPricingService service = new CartPricingService();

	@Test
	@Story("Discounts apply before tax")
	@Severity(SeverityLevel.CRITICAL)
	@Description("Category discount reduces the line first; 8.5% tax is calculated on the discounted subtotal.")
	public void discountsApplyBeforeTax() {
		CartPricingService.CartTotals totals = service.price(List.of(
			new CartPricingService.PricedLineInput(
				UUID.randomUUID(), "Shirt", new BigDecimal("100.00"), 1, new BigDecimal("10.00")
			)
		));

		assertThat(totals.subtotal()).isEqualByComparingTo("90.00");
		assertThat(totals.salesTax()).isEqualByComparingTo("7.65");
		assertThat(totals.total()).isEqualByComparingTo("97.65");
	}

	@Test
	@Story("Multiple categories")
	@Severity(SeverityLevel.NORMAL)
	public void multipleCategoriesSumCorrectly() {
		CartPricingService.CartTotals totals = service.price(List.of(
			new CartPricingService.PricedLineInput(
				UUID.randomUUID(), "Headphones", new BigDecimal("79.99"), 1, new BigDecimal("5.00")
			),
			new CartPricingService.PricedLineInput(
				UUID.randomUUID(), "Apples", new BigDecimal("4.99"), 2, BigDecimal.ZERO
			)
		));

		assertThat(totals.subtotal()).isEqualByComparingTo("85.97");
		assertThat(totals.salesTax()).isEqualByComparingTo("7.31");
		assertThat(totals.total()).isEqualByComparingTo("93.28");
	}

	@Test
	@Story("Empty cart rejected")
	@Severity(SeverityLevel.NORMAL)
	public void emptyCartRejected() {
		assertThatThrownBy(() -> service.price(List.of()))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
