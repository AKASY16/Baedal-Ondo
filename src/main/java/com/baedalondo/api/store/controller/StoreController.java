package com.baedalondo.api.store.controller;

import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.store.dto.StoreRegisterRequest;
import com.baedalondo.api.store.service.StoreService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    public Long registerStore(@RequestBody StoreRegisterRequest request) {
        Store savedStore = storeService.registerStore(request);
        return savedStore.getId();
    }

}
