package io.github.maximilianopizarro.keycloak;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.io.IOException;
import java.util.Base64;

/**
 * Required Action that forces biometric (facial) enrollment during delegated user creation.
 *
 * When an admin creates a user and assigns this required action, on first login the user
 * is prompted to capture multiple facial images via webcam. These images are sent to the
 * NeuroFace service for training, enrolling the user's biometric profile.
 *
 * Flow:
 * 1. Admin creates user in KC admin console
 * 2. Admin adds "Biometric Enrollment" required action to the user
 * 3. User logs in with temporary credentials
 * 4. User is presented with webcam capture page
 * 5. Multiple facial images are captured and sent to NeuroFace /images
 * 6. NeuroFace /train is called to update the recognition model
 * 7. User attribute "biometric_enrolled" is set to "true"
 * 8. User is added to "biometric-enrolled" group
 */
public class BiometricEnrollmentRequiredAction implements RequiredActionProvider {

    private static final Logger LOG = Logger.getLogger(BiometricEnrollmentRequiredAction.class);
    public static final String PROVIDER_ID = "biometric-enrollment";
    private static final String FTL_TEMPLATE = "biometric-enroll.ftl";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = createEnrollmentForm(context, null);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        UserModel user = context.getUser();
        String username = user.getUsername();

        int imageCount = 0;
        for (String key : formData.keySet()) {
            if (key.startsWith("enrollImage_")) {
                imageCount++;
            }
        }

        if (imageCount == 0) {
            String singleImage = formData.getFirst("enrollImage");
            if (singleImage != null && !singleImage.isBlank()) {
                imageCount = 1;
            }
        }

        if (imageCount == 0) {
            Response challenge = createEnrollmentForm(context,
                    "No facial images were captured. Please capture at least one image.");
            context.challenge(challenge);
            return;
        }

        try {
            NeuroFaceClient client = createClient(context);

            if (!client.isHealthy()) {
                Response challenge = createEnrollmentForm(context,
                        "Biometric service is not available. Please try again later.");
                context.challenge(challenge);
                return;
            }

            if (imageCount == 1) {
                String imageData = formData.getFirst("enrollImage");
                if (imageData == null) {
                    imageData = formData.getFirst("enrollImage_0");
                }
                uploadSingleImage(client, username, imageData, 0);
            } else {
                for (int i = 0; i < imageCount; i++) {
                    String imageData = formData.getFirst("enrollImage_" + i);
                    if (imageData != null && !imageData.isBlank()) {
                        uploadSingleImage(client, username, imageData, i);
                    }
                }
            }

            client.train();

            user.setSingleAttribute("biometric_enrolled", "true");
            user.setSingleAttribute("biometric_enrolled_at", String.valueOf(System.currentTimeMillis()));

            context.getRealm().getGroupsStream()
                    .filter(g -> "biometric-enrolled".equals(g.getName()))
                    .findFirst()
                    .ifPresent(user::joinGroup);

            LOG.infof("Biometric enrollment completed for user '%s' with %d images", username, imageCount);
            context.success();

        } catch (IOException e) {
            LOG.errorf(e, "Biometric enrollment failed for user '%s'", username);
            Response challenge = createEnrollmentForm(context,
                    "Error during biometric enrollment: " + e.getMessage());
            context.challenge(challenge);
        }
    }

    @Override
    public void close() {
    }

    private void uploadSingleImage(NeuroFaceClient client, String username, String rawImage, int index)
            throws IOException {
        String base64 = stripDataUrlPrefix(rawImage);
        byte[] imageBytes = Base64.getDecoder().decode(base64);
        String filename = username + "_enroll_" + index + ".jpg";
        client.uploadImage(username, imageBytes, filename);
    }

    private Response createEnrollmentForm(RequiredActionContext context, String errorMessage) {
        RealmModel realm = context.getRealm();
        String maxImagesStr = realm.getAttribute("neuroface.max.enrollment.images");
        int maxImages = 5;
        if (maxImagesStr != null) {
            try {
                maxImages = Integer.parseInt(maxImagesStr);
            } catch (NumberFormatException ignored) {
            }
        }

        org.keycloak.forms.login.LoginFormsProvider form = context.form()
                .setAttribute("username", context.getUser().getUsername())
                .setAttribute("maxImages", maxImages)
                .setAttribute("requiredImages", 3);
        if (errorMessage != null) {
            form.setError(errorMessage);
        }
        return form.createForm(FTL_TEMPLATE);
    }

    private NeuroFaceClient createClient(RequiredActionContext context) {
        RealmModel realm = context.getRealm();

        String apiUrl = realm.getAttribute("neuroface.api.url");
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = System.getenv("NEUROFACE_API_URL");
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "http://neuroface-backend:8080/api";
        }

        String thresholdStr = realm.getAttribute("neuroface.confidence.threshold");
        double threshold = 65.0;
        if (thresholdStr != null && !thresholdStr.isBlank()) {
            try {
                threshold = Double.parseDouble(thresholdStr);
            } catch (NumberFormatException ignored) {
            }
        }

        return new NeuroFaceClient(apiUrl, threshold);
    }

    private String stripDataUrlPrefix(String dataUrl) {
        if (dataUrl != null && dataUrl.contains(",")) {
            return dataUrl.substring(dataUrl.indexOf(",") + 1);
        }
        return dataUrl;
    }
}
