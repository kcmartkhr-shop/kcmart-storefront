package in.kcmart.backend.catalog.controller;

import in.kcmart.backend.catalog.dto.ProductImageResponse;
import in.kcmart.backend.catalog.entity.ProductImage;
import in.kcmart.backend.catalog.repository.ProductImageRepository;
import in.kcmart.backend.catalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
public class ProductImageController {

    private static final long MAX_IMAGE_SIZE = 4_000_000L;
    private static final int MAX_IMAGES = 6;

    private final ProductRepository products;
    private final ProductImageRepository productImages;
    private final Path uploadDirectory;

    public ProductImageController(
            ProductRepository products,
            ProductImageRepository productImages,
            @Value("${kcmart.upload.directory:uploads/products}")
            String directory
    ) throws IOException {

        this.products = products;
        this.productImages = productImages;
        this.uploadDirectory = Paths.get(directory)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadDirectory);
    }

    @GetMapping("/api/v1/admin/products/{id}/images")
    @Transactional(readOnly = true)
    public List<ProductImageResponse> gallery(
            @PathVariable Long id
    ) {
        if (!products.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found"
            );
        }

        return productImages
                .findByProductIdOrderByDisplayOrderAscIdAsc(id)
                .stream()
                .map(ProductImageResponse::from)
                .toList();
    }

    @PostMapping(
            value = {
                    "/api/v1/admin/products/{id}/image",
                    "/api/v1/admin/products/{id}/images"
            },
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Transactional
    public ProductImageResponse upload(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        var product = products.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        var existing = productImages.findByProductIdForUpdate(id);

        if (existing.size() >= MAX_IMAGES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A product can have a maximum of 6 images"
            );
        }

        ImageType imageType = validateImage(file);

        String filename = UUID.randomUUID() + imageType.extension();
        Path destination = uploadDirectory.resolve(filename).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid image destination"
            );
        }

        try (var input = file.getInputStream()) {
            Files.copy(
                    input,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        String imageUrl = "/api/v1/product-images/" + filename;

        try {
            ProductImage image = new ProductImage();
            image.setProductId(id);
            image.setImageUrl(imageUrl);
            image.setDisplayOrder(nextDisplayOrder(existing));

            boolean firstImage = existing.isEmpty();
            image.setCover(firstImage);

            ProductImage saved = productImages.saveAndFlush(image);

            if (firstImage) {
                product.setImageUrl(imageUrl);
                products.save(product);
            }

            return ProductImageResponse.from(saved);

        } catch (RuntimeException ex) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException ignored) {
            }

            throw ex;
        }
    }

    @PatchMapping(
            "/api/v1/admin/products/{productId}/images/{imageId}/cover"
    )
    @Transactional
    public ProductImageResponse setCover(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        var product = products.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        var images = productImages.findByProductIdForUpdate(productId);

        ProductImage selected = images.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product image not found"
                ));

        for (ProductImage image : images) {
            image.setCover(image.getId().equals(imageId));
        }

        productImages.saveAll(images);

        product.setImageUrl(selected.getImageUrl());
        products.save(product);

        return ProductImageResponse.from(selected);
    }

    @DeleteMapping(
            "/api/v1/admin/products/{productId}/images/{imageId}"
    )
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        var product = products.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        var images = productImages.findByProductIdForUpdate(productId);

        ProductImage target = images.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product image not found"
                ));

        boolean deletingCover = target.isCover();
        String deletedImageUrl = target.getImageUrl();

        productImages.delete(target);
        productImages.flush();

        var remaining = images.stream()
                .filter(image -> !image.getId().equals(imageId))
                .sorted(
                        Comparator.comparingInt(ProductImage::getDisplayOrder)
                                .thenComparing(ProductImage::getId)
                )
                .toList();

        if (remaining.isEmpty()) {
            product.setImageUrl(null);
        } else if (deletingCover) {
            ProductImage replacement = remaining.get(0);
            replacement.setCover(true);
            productImages.save(replacement);
            product.setImageUrl(replacement.getImageUrl());
        }

        products.save(product);

        deletePhysicalFile(deletedImageUrl);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/product-images/{filename}")
    public ResponseEntity<byte[]> getImage(
            @PathVariable String filename
    ) throws IOException {

        if (!filename.matches(
                "[a-fA-F0-9-]{36}\\.(jpg|png|webp)"
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid filename"
            );
        }

        Path image = uploadDirectory.resolve(filename).normalize();

        if (!image.startsWith(uploadDirectory)
                || !Files.isRegularFile(image)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Image not found"
            );
        }

        String type = filename.endsWith(".png")
                ? "image/png"
                : filename.endsWith(".webp")
                    ? "image/webp"
                    : "image/jpeg";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type))
                .header(
                        HttpHeaders.CACHE_CONTROL,
                        "public, max-age=86400"
                )
                .body(Files.readAllBytes(image));
    }

    private ImageType validateImage(
            MultipartFile file
    ) throws IOException {

        if (file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image must be between 1 byte and 4 MB"
            );
        }

        byte[] header = new byte[12];
        int read;

        try (var stream = file.getInputStream()) {
            read = stream.read(header);
        }

        if (read < 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid image"
            );
        }

        if ((header[0] & 255) == 255
                && (header[1] & 255) == 216
                && (header[2] & 255) == 255) {
            return new ImageType(".jpg", "image/jpeg");
        }

        if (read >= 8
                && (header[0] & 255) == 137
                && header[1] == 80
                && header[2] == 78
                && header[3] == 71) {
            return new ImageType(".png", "image/png");
        }

        if (read >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return new ImageType(".webp", "image/webp");
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only JPG, PNG and WebP are supported"
        );
    }

    private int nextDisplayOrder(
            List<ProductImage> images
    ) {
        return images.stream()
                .mapToInt(ProductImage::getDisplayOrder)
                .max()
                .orElse(-1) + 1;
    }

    private void deletePhysicalFile(
            String imageUrl
    ) {
        if (imageUrl == null) {
            return;
        }

        String prefix = "/api/v1/product-images/";

        if (!imageUrl.startsWith(prefix)) {
            return;
        }

        String filename = imageUrl.substring(prefix.length());

        if (!filename.matches(
                "[a-fA-F0-9-]{36}\\.(jpg|png|webp)"
        )) {
            return;
        }

        Path path = uploadDirectory.resolve(filename).normalize();

        if (!path.startsWith(uploadDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private record ImageType(
            String extension,
            String contentType
    ) {
    }
}