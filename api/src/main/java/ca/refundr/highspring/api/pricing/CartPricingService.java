package ca.refundr.highspring.api.pricing;

import com.google.common.base.Preconditions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a shopping basket into money totals.
 * Category discounts are applied first; sales tax is added only after that.
 */
public final class CartPricingService {

	public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.085");
	private static final int MONEY_SCALE = 2;

	private final BigDecimal taxRate;

	public CartPricingService() {
		this(DEFAULT_TAX_RATE);
	}

	public CartPricingService(BigDecimal taxRate) {
		this.taxRate = Preconditions.checkNotNull(taxRate, "taxRate");
		Preconditions.checkArgument(taxRate.compareTo(BigDecimal.ZERO) >= 0, "taxRate must be >= 0");
	}

	/**
	 * One product the shopper wants, with the price and category discount already known.
	 *
	 * @param productId        catalog product id
	 * @param productName      display name for the line
	 * @param unitPrice        list price per unit before discount
	 * @param quantity         units requested (must be &gt; 0)
	 * @param discountPercent  category discount percent (0–100)
	 */
	public record PricedLineInput(
		java.util.UUID productId,
		String productName,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal discountPercent
	) {
	}

	/**
	 * One line after the discount math is done.
	 *
	 * @param productId        catalog product id
	 * @param productName      display name for the line
	 * @param unitPrice        unit list price (scaled for money)
	 * @param quantity         units on this line
	 * @param discountPercent  discount that was applied (0–100)
	 * @param lineSubtotal     money for this line after discount, before tax
	 */
	public record PricedLine(
		java.util.UUID productId,
		String productName,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal discountPercent,
		BigDecimal lineSubtotal
	) {
	}

	/**
	 * Full cart money picture: lines, subtotal (after discounts), tax, and grand total.
	 *
	 * @param lines     priced lines in order
	 * @param subtotal  sum of line subtotals after discounts
	 * @param salesTax  tax on {@code subtotal}
	 * @param total     {@code subtotal + salesTax}
	 * @param taxRate   rate used to compute {@code salesTax}
	 */
	public record CartTotals(
		List<PricedLine> lines,
		BigDecimal subtotal,
		BigDecimal salesTax,
		BigDecimal total,
		BigDecimal taxRate
	) {
	}

	public CartTotals price(List<PricedLineInput> inputs) {
		Preconditions.checkNotNull(inputs, "inputs");
		Preconditions.checkArgument(!inputs.isEmpty(), "Cart must contain at least one item");

		List<PricedLine> lines = new ArrayList<>();
		BigDecimal subtotal = BigDecimal.ZERO;

		for (PricedLineInput input : inputs) {
			Preconditions.checkNotNull(input, "input");
			Preconditions.checkArgument(input.quantity() > 0, "Quantity must be a whole number greater than zero");
			Preconditions.checkArgument(input.unitPrice().compareTo(BigDecimal.ZERO) >= 0, "Price cannot be negative");
			Preconditions.checkArgument(
				input.discountPercent().compareTo(BigDecimal.ZERO) >= 0
					&& input.discountPercent().compareTo(new BigDecimal("100")) <= 0,
				"Discount must be between 0 and 100"
			);

			BigDecimal gross = input.unitPrice()
				.multiply(BigDecimal.valueOf(input.quantity()));
			BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
				input.discountPercent().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
			);
			BigDecimal lineSubtotal = gross.multiply(discountMultiplier).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
			lines.add(new PricedLine(
				input.productId(),
				input.productName(),
				input.unitPrice().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
				input.quantity(),
				input.discountPercent().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
				lineSubtotal
			));
			subtotal = subtotal.add(lineSubtotal);
		}

		subtotal = subtotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		BigDecimal salesTax = subtotal.multiply(taxRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		BigDecimal total = subtotal.add(salesTax).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
		return new CartTotals(List.copyOf(lines), subtotal, salesTax, total, taxRate);
	}
}
