CREATE INDEX idx_products_active_created_at
    ON products (created_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX idx_products_active_price
    ON products (price)
    WHERE deleted = FALSE;

CREATE INDEX idx_products_active_stock
    ON products (stock_quantity)
    WHERE deleted = FALSE;