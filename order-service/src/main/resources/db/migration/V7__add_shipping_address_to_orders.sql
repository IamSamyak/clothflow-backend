ALTER TABLE orders
    ADD COLUMN recipient_name VARCHAR(200),
    ADD COLUMN shipping_address_line1 VARCHAR(255),
    ADD COLUMN shipping_address_line2 VARCHAR(255),
    ADD COLUMN shipping_city VARCHAR(100),
    ADD COLUMN shipping_state VARCHAR(100),
    ADD COLUMN shipping_postal_code VARCHAR(20),
    ADD COLUMN shipping_country VARCHAR(100);