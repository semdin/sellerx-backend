-- Add commission_rate and shipping_volume_weight columns to trendyol_products table
ALTER TABLE trendyol_products 
ADD COLUMN commission_rate DECIMAL(5,2),
ADD COLUMN shipping_volume_weight DECIMAL(5,2);

-- Add comments to explain the columns
COMMENT ON COLUMN trendyol_products.commission_rate IS 'Commission rate from category information';
COMMENT ON COLUMN trendyol_products.shipping_volume_weight IS 'Shipping volume weight from category information';
