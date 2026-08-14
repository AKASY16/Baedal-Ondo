package com.baedalondo.api;

import com.baedalondo.api.legal.LegalController;
import com.baedalondo.api.legal.LegalDocumentVersions;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegalControllerTest {

    private final LegalController legalController =
            new LegalController("배달온도 운영자", "privacy@example.com");

    @Test
    void termsPageUsesCurrentTermsVersion() {
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = legalController.terms(model);

        assertEquals("legal/terms", viewName);
        assertEquals(LegalDocumentVersions.TERMS, model.get("documentVersion"));
        assertEquals("배달온도 운영자", model.get("operatorName"));
    }

    @Test
    void privacyPageUsesCurrentPrivacyVersion() {
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = legalController.privacy(model);

        assertEquals("legal/privacy", viewName);
        assertEquals(LegalDocumentVersions.PRIVACY, model.get("documentVersion"));
        assertEquals("privacy@example.com", model.get("contactEmail"));
    }
}
