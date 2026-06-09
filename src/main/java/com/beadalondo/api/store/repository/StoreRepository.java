package com.beadalondo.api.store.repository;

import com.beadalondo.api.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByOwnerId(Long ownerId);

    Optional<Store> findFirstByOwnerId(Long ownerId);

    Optional<Store> findByIdAndOwnerId(Long storeId, Long ownerId);

}