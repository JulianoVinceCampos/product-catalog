CREATE TABLE products (
    id          BINARY(16)     NOT NULL,
    sku         VARCHAR(100)   NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    price       DECIMAL(19, 2) NOT NULL,
    category    VARCHAR(100),
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    image_url   VARCHAR(1000),
    created_at  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted     TINYINT(1)     NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uk_products_sku (sku),
    INDEX idx_products_status     (status),
    INDEX idx_products_category   (category),
    INDEX idx_products_deleted    (deleted),
    INDEX idx_products_created_at (created_at DESC)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
