ALTER TABLE order_inventory_reservations
DROP CONSTRAINT chk_order_inventory_reservation_status;

ALTER TABLE order_inventory_reservations
ADD CONSTRAINT chk_order_inventory_reservation_status
CHECK (
    status IN (
        'PENDING',
        'RESERVED',
        'COMMITTED',
        'RELEASED',
        'RESTORED',
        'FAILED',
        'RELEASE_FAILED'
    )
);