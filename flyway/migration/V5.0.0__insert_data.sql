-- V5.0.0__insert_data.sql

-- Insert categories (idempotent)
INSERT INTO categories (name, description)
VALUES
  ('Electronics', 'Electronic devices and accessories'),
  ('Clothing', 'Fashion and apparel'),
  ('Books', 'Books and educational materials'),
  ('Home & Garden', 'Home improvement and garden supplies'),
  ('Sports', 'Sports equipment and accessories'),
  ('Beauty', 'Beauty and personal care products'),
  ('Toys', 'Toys and games for all ages'),
  ('Automotive', 'Automotive parts and accessories')
ON CONFLICT (name) DO NOTHING;

-- Insert 1000 dummy products (idempotent for sku using ON CONFLICT DO NOTHING)
-- This uses generate_series to create deterministic SKUs and distribution across the categories above.
INSERT INTO products (uuid, name, description, price, quantity, category_id, sku, image_url, is_available, created_at, updated_at)
SELECT
  uuid_generate_v4() as uuid,
  'Product ' || gs as name,
  'Auto-generated product description for product ' || gs as description,
  round((random() * 1000)::numeric, 2) as price,
  (random() * 200)::int as quantity,
  ((gs - 1) % 8) + 1 as category_id,                         -- distribute across 8 categories
  'SKU-' || lpad(gs::text, 6, '0') as sku,                   -- SKU like SKU-000001
  'https://example.com/images/' || gs || '.jpg' as image_url,
  (random() > 0.1) as is_available,                          -- ~90% available
  NOW() as created_at,
  NOW() as updated_at
FROM generate_series(1, 1000) AS gs
ON CONFLICT (sku) DO NOTHING;
