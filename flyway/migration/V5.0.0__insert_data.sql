-- Insert categories
INSERT INTO categories (name, description) VALUES
('Electronics', 'Electronic devices and accessories'),
('Clothing', 'Fashion and apparel'),
('Books', 'Books and educational materials'),
('Home & Garden', 'Home improvement and garden supplies'),
('Sports', 'Sports equipment and accessories'),
('Beauty', 'Beauty and personal care products'),
('Toys', 'Toys and games for all ages'),
('Automotive', 'Automotive parts and accessories');

-- Insert 1000 dummy products
INSERT INTO products (name, description, price, stock_quantity, category_id, sku, image_url)
SELECT
    'Product ' || seq,
    'This is a detailed description for product ' || seq || '. This product offers great value and quality.',
    (RANDOM() * 1000)::DECIMAL(10,2),
    FLOOR(RANDOM() * 1000)::INTEGER,
    FLOOR(1 + RANDOM() * 7)::BIGINT,  -- Random category between 1-8
    'SKU-' || LPAD(seq::TEXT, 6, '0'),
    'https://example.com/images/product-' || seq || '.jpg'
FROM generate_series(1, 1000) seq;

-- Update some products to be inactive
UPDATE products SET is_active = false WHERE id % 20 = 0;  -- 5% inactive

-- Create a materialized view for frequently accessed product data (optional)
CREATE MATERIALIZED VIEW product_catalog AS
SELECT
    p.id,
    p.name,
    p.description,
    p.price,
    p.stock_quantity,
    p.sku,
    p.image_url,
    p.is_active,
    c.name as category_name,
    p.created_at,
    p.updated_at
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.is_active = true;

-- Create index on materialized view
CREATE UNIQUE INDEX idx_product_catalog_id ON product_catalog(id);
CREATE INDEX idx_product_catalog_category ON product_catalog(category_name);