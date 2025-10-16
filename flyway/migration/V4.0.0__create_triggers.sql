-- V4.0.0__create_triggers.sql
-- Trigger function to keep updated_at current on UPDATE
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger to products table
DROP TRIGGER IF EXISTS trg_update_products_updated_at ON products;
CREATE TRIGGER trg_update_products_updated_at
BEFORE UPDATE ON products
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Also attach to categories (optional)
DROP TRIGGER IF EXISTS trg_update_categories_updated_at ON categories;
CREATE TRIGGER trg_update_categories_updated_at
BEFORE UPDATE ON categories
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
