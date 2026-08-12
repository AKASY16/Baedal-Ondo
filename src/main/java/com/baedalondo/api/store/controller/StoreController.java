package com.baedalondo.api.store.controller;

import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.store.dto.StoreEditRequest;
import com.baedalondo.api.store.dto.StoreRegisterRequest;
import com.baedalondo.api.store.service.StoreService;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{storeId}")
    public Long editStore(
            @PathVariable("storeId") Long storeId,
            @RequestBody StoreEditRequest request
    ) {
        Store editedStore = storeService.editStore(storeId, request);

        return editedStore.getId();
    }
}