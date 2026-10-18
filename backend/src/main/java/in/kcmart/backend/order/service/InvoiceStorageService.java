package in.kcmart.backend.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class InvoiceStorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    private final Path storageRoot;

    public InvoiceStorageService(
            @Value("${kcmart.invoice.storage-path:data/invoices}")
            String storagePath) {

        try {
            this.storageRoot = Path.of(storagePath)
                .toAbsolutePath()
                .normalize();

            Files.createDirectories(this.storageRoot);
        }
        catch (IOException ex) {
            throw new IllegalStateException(
                "Could not initialize invoice storage.",
                ex
            );
        }
    }

    public StoredInvoice store(MultipartFile file) {

        validate(file);

        String storageKey =
            UUID.randomUUID().toString().replace("-", "") + ".pdf";

        Path destination = resolveStorageKey(storageKey);

        try (InputStream input = file.getInputStream()) {

            Files.copy(
                input,
                destination,
                StandardCopyOption.REPLACE_EXISTING
            );

            return new StoredInvoice(
                storageKey,
                safeOriginalFilename(file.getOriginalFilename()),
                "application/pdf",
                file.getSize()
            );
        }
        catch (IOException ex) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not store invoice file."
            );
        }
    }

    public Resource load(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Invoice file not found."
            );
        }

        Path target = resolveStorageKey(storageKey);

        if (!Files.exists(target)
                || !Files.isRegularFile(target)
                || !Files.isReadable(target)) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Invoice file not found."
            );
        }

        try {
            Resource resource = new UrlResource(target.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Invoice file not found."
                );
            }

            return resource;
        }
        catch (MalformedURLException ex) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not load invoice file."
            );
        }
    }

    public void deleteQuietly(String storageKey) {

        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        final Path target;

        try {
            target = resolveStorageKey(storageKey);
        }
        catch (ResponseStatusException ex) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        }
        catch (IOException ignored) {
            // Cleanup failure must not hide the primary operation result.
        }
    }

    private Path resolveStorageKey(String storageKey) {

        Path target = storageRoot
            .resolve(storageKey)
            .normalize();

        if (!target.startsWith(storageRoot)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid invoice storage path."
            );
        }

        return target;
    }

    private void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invoice PDF is required."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invoice PDF must not exceed 10 MB."
            );
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only PDF invoice files are allowed."
            );
        }

        try (InputStream input = file.getInputStream()) {

            byte[] signature = input.readNBytes(5);

            if (signature.length != 5
                    || signature[0] != '%'
                    || signature[1] != 'P'
                    || signature[2] != 'D'
                    || signature[3] != 'F'
                    || signature[4] != '-') {

                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Uploaded file is not a valid PDF."
                );
            }
        }
        catch (IOException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Could not validate invoice PDF."
            );
        }
    }

    private String safeOriginalFilename(String originalFilename) {

        if (originalFilename == null || originalFilename.isBlank()) {
            return "invoice.pdf";
        }

        String filename = Path.of(originalFilename)
            .getFileName()
            .toString()
            .trim();

        if (filename.isBlank()) {
            return "invoice.pdf";
        }

        if (filename.length() > 255) {
            filename = filename.substring(filename.length() - 255);
        }

        return filename;
    }

    public record StoredInvoice(
        String storageKey,
        String originalFilename,
        String contentType,
        long fileSize
    ) {
    }
}