package com.baedalondo.api.store.controller;

import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.store.service.StoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class StoreEditPageController {

    private final String jusoPopupAuthKey;
    private final StoreService storeService;

    public StoreEditPageController(
            @Value("${jusogokr.api.popup-auth-key:TESTJUSOGOKR}")
            String jusoPopupAuthKey,
            StoreService storeService
    ) {
        this.jusoPopupAuthKey = jusoPopupAuthKey;
        this.storeService = storeService;
    }

    @RequestMapping(
            value = "/store/{storeId}/edit",
            method = {RequestMethod.GET, RequestMethod.POST}
    )
    public String editPage(
            @PathVariable("storeId") Long storeId,
            Model model
    ) {
        Store store = storeService.getCurrentUserStoreById(storeId);

        model.addAttribute("store", store);
        model.addAttribute("jusoPopupAuthKey", jusoPopupAuthKey);

        return "store/edit";
    }
}