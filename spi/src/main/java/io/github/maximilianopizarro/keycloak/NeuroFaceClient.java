package io.github.maximilianopizarro.keycloak;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for NeuroFace facial recognition API.
 * Communicates with the NeuroFace backend deployed in the same namespace.
 */
public class NeuroFaceClient {

    private static final Logger LOG = Logger.getLogger(NeuroFaceClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final double confidenceThreshold;

    public NeuroFaceClient(String baseUrl, double confidenceThreshold) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.confidenceThreshold = confidenceThreshold;
    }

    /**
     * Recognize faces in a base64-encoded image.
     * POST /recognize { "image": "<base64>" }
     *
     * @return list of recognized face results
     */
    public List<FaceResult> recognize(String base64Image) throws IOException {
        String payload = MAPPER.writeValueAsString(new RecognizeRequest(base64Image));

        HttpURLConnection conn = openConnection("/recognize", "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            String error = readStream(conn.getErrorStream());
            LOG.warnf("NeuroFace /recognize returned %d: %s", status, error);
            throw new IOException("NeuroFace recognition failed with status " + status);
        }

        JsonNode root = MAPPER.readTree(conn.getInputStream());
        List<FaceResult> results = new ArrayList<>();
        for (JsonNode face : root.get("faces")) {
            results.add(new FaceResult(
                    face.get("label").asText(),
                    face.get("confidence").asDouble(),
                    face.get("x").asInt(),
                    face.get("y").asInt(),
                    face.get("w").asInt(),
                    face.get("h").asInt()
            ));
        }
        return results;
    }

    /**
     * Upload a training image for a label.
     * POST /images (multipart: label + file)
     */
    public void uploadImage(String label, byte[] imageData, String filename) throws IOException {
        String boundary = "----NeuroFaceBoundary" + System.currentTimeMillis();

        HttpURLConnection conn = openConnection("/images", "POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            writeMultipartField(os, boundary, "label", label);
            writeMultipartFile(os, boundary, "file", filename, "image/jpeg", imageData);
            os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            String error = readStream(conn.getErrorStream());
            LOG.warnf("NeuroFace /images upload returned %d: %s", status, error);
            throw new IOException("NeuroFace image upload failed with status " + status);
        }

        LOG.infof("Uploaded biometric image for label '%s'", label);
    }

    /**
     * Trigger model training after uploading images.
     * POST /train
     */
    public void train() throws IOException {
        HttpURLConnection conn = openConnection("/train", "POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write("{}".getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            String error = readStream(conn.getErrorStream());
            LOG.warnf("NeuroFace /train returned %d: %s", status, error);
            throw new IOException("NeuroFace training failed with status " + status);
        }

        LOG.info("NeuroFace model training triggered successfully");
    }

    /**
     * Check if NeuroFace backend is healthy.
     * GET /health
     */
    public boolean isHealthy() {
        try {
            HttpURLConnection conn = openConnection("/health", "GET");
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            LOG.warnf("NeuroFace health check failed: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Check if a recognized face matches the expected username with sufficient confidence.
     */
    public boolean verifyIdentity(String base64Image, String expectedUsername) throws IOException {
        List<FaceResult> faces = recognize(base64Image);

        if (faces.isEmpty()) {
            LOG.info("No faces detected in the image");
            return false;
        }

        for (FaceResult face : faces) {
            if (face.label().equalsIgnoreCase(expectedUsername) && face.confidence() >= confidenceThreshold) {
                LOG.infof("Biometric verification successful for '%s' (confidence: %.2f)",
                        expectedUsername, face.confidence());
                return true;
            }
        }

        LOG.infof("Biometric verification failed for '%s'. Detected faces: %s", expectedUsername, faces);
        return false;
    }

    private HttpURLConnection openConnection(String path, String method) throws IOException {
        URI uri = URI.create(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);
        return conn;
    }

    private String readStream(java.io.InputStream is) {
        if (is == null) return "";
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "(unreadable)";
        }
    }

    private void writeMultipartField(OutputStream os, String boundary, String name, String value)
            throws IOException {
        String field = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        os.write(field.getBytes(StandardCharsets.UTF_8));
    }

    private void writeMultipartFile(OutputStream os, String boundary, String fieldName,
                                    String filename, String contentType, byte[] data) throws IOException {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        os.write(header.getBytes(StandardCharsets.UTF_8));
        os.write(data);
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    public record RecognizeRequest(String image) {}

    public record FaceResult(String label, double confidence, int x, int y, int w, int h) {}
}
