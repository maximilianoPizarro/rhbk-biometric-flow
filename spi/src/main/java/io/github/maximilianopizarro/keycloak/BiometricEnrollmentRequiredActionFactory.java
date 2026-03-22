package io.github.maximilianopizarro.keycloak;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class BiometricEnrollmentRequiredActionFactory implements RequiredActionFactory {

    @Override
    public String getId() {
        return BiometricEnrollmentRequiredAction.PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "NeuroFace Biometric Enrollment";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new BiometricEnrollmentRequiredAction();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }
}
