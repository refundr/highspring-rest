-- Shopping cart schema: catalog, users/sessions, purchases, and 500 error log.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_role AS ENUM ('CUSTOMER', 'ADMIN');

CREATE TABLE category (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	code VARCHAR(64) NOT NULL UNIQUE,
	name VARCHAR(128) NOT NULL,
	discount_percent NUMERIC(5, 2) NOT NULL CHECK (discount_percent >= 0 AND discount_percent <= 100)
);

CREATE TABLE product (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	category_id UUID NOT NULL REFERENCES category (id),
	name VARCHAR(256) NOT NULL,
	unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
	active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE app_user (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	google_sub VARCHAR(255) NOT NULL UNIQUE,
	email VARCHAR(320) NOT NULL UNIQUE,
	display_name VARCHAR(256),
	role user_role NOT NULL DEFAULT 'CUSTOMER',
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE api_session (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
	expires_at TIMESTAMPTZ NOT NULL,
	last_activity_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX api_session_user_id_idx ON api_session (user_id);

CREATE TABLE purchase (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	user_id UUID NOT NULL REFERENCES app_user (id),
	subtotal NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
	sales_tax NUMERIC(12, 2) NOT NULL CHECK (sales_tax >= 0),
	total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
	tax_rate NUMERIC(6, 4) NOT NULL CHECK (tax_rate >= 0),
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX purchase_user_id_idx ON purchase (user_id);
CREATE INDEX purchase_created_at_idx ON purchase (created_at DESC);

CREATE TABLE purchase_item (
	id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
	purchase_id UUID NOT NULL REFERENCES purchase (id) ON DELETE CASCADE,
	product_id UUID NOT NULL REFERENCES product (id),
	product_name VARCHAR(256) NOT NULL,
	unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
	quantity INTEGER NOT NULL CHECK (quantity > 0),
	discount_percent NUMERIC(5, 2) NOT NULL CHECK (discount_percent >= 0 AND discount_percent <= 100),
	line_subtotal NUMERIC(12, 2) NOT NULL CHECK (line_subtotal >= 0)
);

CREATE INDEX purchase_item_purchase_id_idx ON purchase_item (purchase_id);

CREATE TABLE api_error_log (
	id BIGSERIAL PRIMARY KEY,
	level VARCHAR(16) NOT NULL,
	logger_name VARCHAR(255),
	message TEXT,
	stack_trace TEXT,
	request_method VARCHAR(16),
	request_path TEXT,
	user_id UUID,
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed catalog: three categories with different discounts, sample products.
INSERT INTO category (id, code, name, discount_percent) VALUES
	('11111111-1111-1111-1111-111111111111', 'ELECTRONICS', 'Electronics', 5.00),
	('22222222-2222-2222-2222-222222222222', 'CLOTHING', 'Clothing', 10.00),
	('33333333-3333-3333-3333-333333333333', 'GROCERY', 'Grocery', 0.00);

INSERT INTO product (id, category_id, name, unit_price, active) VALUES
	('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Wireless Headphones', 79.99, TRUE),
	('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'USB-C Hub', 34.50, TRUE),
	('cccccccc-cccc-cccc-cccc-cccccccccccc', '22222222-2222-2222-2222-222222222222', 'Cotton T-Shirt', 19.99, TRUE),
	('dddddddd-dddd-dddd-dddd-dddddddddddd', '22222222-2222-2222-2222-222222222222', 'Denim Jacket', 89.00, TRUE),
	('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '33333333-3333-3333-3333-333333333333', 'Organic Apples (1kg)', 4.99, TRUE),
	('ffffffff-ffff-ffff-ffff-ffffffffffff', '33333333-3333-3333-3333-333333333333', 'Sourdough Loaf', 6.50, TRUE);
