package com.beadalondo.api.store.repository;

import com.beadalondo.api.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

}