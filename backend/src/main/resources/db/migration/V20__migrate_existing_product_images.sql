INSERT INTO product_images (
    product_id,
    image_url,
    is_cover,
    display_order
)
SELECT
    p.id,
    p.image_url,
    TRUE,
    0
FROM products p
WHERE p.image_url IS NOT NULL
  AND BTRIM(p.image_url) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM product_images pi
      WHERE pi.product_id = p.id
  );