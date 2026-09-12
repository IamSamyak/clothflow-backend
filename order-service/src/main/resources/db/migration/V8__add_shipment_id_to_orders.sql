ALTER TABLE orders
    ADD COLUMN shipment_id UUID;

CREATE UNIQUE INDEX uk_orders_shipment_id
    ON orders(shipment_id)
    WHERE shipment_id IS NOT NULL;