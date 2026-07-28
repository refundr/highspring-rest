-- Persist a per-user shopping cart and add product images for the catalog UI.

ALTER TABLE product
	ADD COLUMN image_url TEXT;

UPDATE product SET image_url = CASE id
	WHEN 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
		THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&h=800&q=80'
	WHEN 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
		THEN 'https://images.unsplash.com/photo-1625948515291-69613efd103f?auto=format&fit=crop&w=800&h=800&q=80'
	WHEN 'cccccccc-cccc-cccc-cccc-cccccccccccc'
		THEN 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=800&h=800&q=80'
	WHEN 'dddddddd-dddd-dddd-dddd-dddddddddddd'
		THEN 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&h=800&q=80'
	WHEN 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'
		THEN 'https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=800&h=800&q=80'
	WHEN 'ffffffff-ffff-ffff-ffff-ffffffffffff'
		THEN 'https://images.unsplash.com/photo-1549931319-a545dcf3bc73?auto=format&fit=crop&w=800&h=800&q=80'
	END
WHERE image_url IS NULL;

ALTER TABLE product
	ALTER COLUMN image_url SET NOT NULL;

CREATE TABLE cart_item (
	user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
	product_id UUID NOT NULL REFERENCES product (id),
	quantity INTEGER NOT NULL CHECK (quantity > 0),
	updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (user_id, product_id)
);

CREATE INDEX cart_item_user_id_idx ON cart_item (user_id);
