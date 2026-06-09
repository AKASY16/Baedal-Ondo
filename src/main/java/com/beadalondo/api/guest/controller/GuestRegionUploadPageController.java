package com.beadalondo.api.guest.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class GuestRegionUploadPageController {

    private final String jusoPopupAuthKey;

    public GuestRegionUploadPageController(
            @Value("${jusogokr.api.popup-auth-key:TESTJUSOGOKR}") String jusoPopupAuthKey
    ) {
        this.jusoPopupAuthKey = jusoPopupAuthKey;
    }

    @RequestMapping(value = "/guestregionuploadpage", method = {RequestMethod.GET, RequestMethod.POST})
    public String uploadPage(Model model) {
        model.addAttribute("jusoPopupAuthKey", jusoPopupAuthKey);
        return "guestregionuploadpage";
    }
}
