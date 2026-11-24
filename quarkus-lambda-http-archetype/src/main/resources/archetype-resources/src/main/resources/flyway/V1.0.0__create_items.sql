-- Example table of the archetype: replace it with the schema of your service
CREATE TABLE items
(
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)   NOT NULL,
    description VARCHAR(500)   NULL,
    unit_price  DECIMAL(10, 2) NOT NULL,
    created_at  DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_items_name UNIQUE (name)
);
