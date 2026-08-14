package com.baedalondo.api.legal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    private static final String UNCONFIGURED_CONTACT = "배포 전 설정 필요";

    private final String operatorName;
    private final String contactEmail;

    public LegalController(
            @Value("${service.legal.operator-name:배달온도 운영자}") String operatorName,
            @Value("${service.legal.contact-email:배포 전 설정 필요}") String contactEmail) {
        this.operatorName = operatorName;
        this.contactEmail = contactEmail;
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        addCommonAttributes(model, LegalDocumentVersions.TERMS);
        return "legal/terms";
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        addCommonAttributes(model, LegalDocumentVersions.PRIVACY);
        return "legal/privacy";
    }

    private void addCommonAttributes(Model model, String documentVersion) {
        model.addAttribute("operatorName", operatorName);
        model.addAttribute("contactEmail", contactEmail);
        model.addAttribute("contactConfigured", !UNCONFIGURED_CONTACT.equals(contactEmail));
        model.addAttribute("documentVersion", documentVersion);
    }
}
