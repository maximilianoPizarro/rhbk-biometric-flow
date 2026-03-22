package io.github.maximilianopizarro.keycloak;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.io.IOException;

/**
 * Keycloak Authenticator SPI that performs biometric second-factor authentication
 * using the NeuroFace facial recognition service.
 *
 * Flow:
 * 1. User enters username/password (standard KC form)
 * 2. This authenticator presents the webcam capture page
 * 3. User captures their face image via browser webcam
 * 4. Image is sent to NeuroFace /recognize endpoint
 * 5. If the recognized label matches the authenticated username → success
 * 6. Otherwise → authentication denied
 */
public class BiometricAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(BiometricAuthenticator.class);
    private static final String FTL_TEMPLATE = "biometric-login.ftl";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        if (user == null) {
            context.failure(AuthenticationFlowError.UNKNOWN_USER);
            return;
        }

        String enrolled = user.getFirstAttribute("biometric_enrolled");
        if (!"true".equalsIgnoreCase(enrolled)) {
            LOG.infof("User '%s' has no biometric enrollment, skipping 2FA", user.getUsername());
            context.success();
            return;
        }

        Response challenge = createBiometricForm(context, null);
        context.challenge(challenge);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String biometricImage = formData.getFirst("biometricImage");

        if (biometricImage == null || biometricImage.isBlank()) {
            Response challenge = createBiometricForm(context, "No facial image captured. Please try again.");
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            return;
        }

        String base64Image = stripDataUrlPrefix(biometricImage);
        UserModel user = context.getUser();
        String username = user.getUsername();

        try {
            NeuroFaceClient client = createClient(context);

            if (!client.isHealthy()) {
                LOG.warn("NeuroFace service is not available, allowing authentication to proceed");
                context.success();
                return;
            }

            boolean verified = client.verifyIdentity(base64Image, username);

            if (verified) {
                LOG.infof("Biometric 2FA passed for user '%s'", username);
                context.success();
            } else {
                LOG.warnf("Biometric 2FA failed for user '%s'", username);
                Response challenge = createBiometricForm(context,
                        "Facial recognition did not match. Please ensure good lighting and face the camera directly.");
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challenge);
            }
        } catch (IOException e) {
            LOG.errorf(e, "Error communicating with NeuroFace for user '%s'", username);
            Response challenge = createBiometricForm(context,
                    "Biometric service temporarily unavailable. Please try again.");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return "true".equalsIgnoreCase(user.getFirstAttribute("biometric_enrolled"));
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        if (!"true".equalsIgnoreCase(user.getFirstAttribute("biometric_enrolled"))) {
            user.addRequiredAction("biometric-enrollment");
        }
    }

    @Override
    public void close() {
    }

    private Response createBiometricForm(AuthenticationFlowContext context, String errorMessage) {
        LoginFormsProvider form = context.form();
        form.setAttribute("username", context.getUser().getUsername());
        if (errorMessage != null) {
            form.setError(errorMessage);
        }
        return form.createForm(FTL_TEMPLATE);
    }

    private NeuroFaceClient createClient(AuthenticationFlowContext context) {
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
        if (dataUrl.contains(",")) {
            return dataUrl.substring(dataUrl.indexOf(",") + 1);
        }
        return dataUrl;
    }
}
