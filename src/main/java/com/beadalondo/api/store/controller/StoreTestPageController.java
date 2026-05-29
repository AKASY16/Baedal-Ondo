package com.beadalondo.api.store.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class StoreTestPageController {

    private final String jusoPopupAuthKey;

    public StoreTestPageController(
            @Value("${jusogokr.api.popup-auth-key:TESTJUSOGOKR}") String jusoPopupAuthKey
    ) {
        this.jusoPopupAuthKey = jusoPopupAuthKey;
    }

    @RequestMapping(value = "/testingpage", method = {RequestMethod.GET, RequestMethod.POST})
    public String testingPage(Model model) {
        model.addAttribute("jusoPopupAuthKey", jusoPopupAuthKey);

        return "testingpage";
    }
}
