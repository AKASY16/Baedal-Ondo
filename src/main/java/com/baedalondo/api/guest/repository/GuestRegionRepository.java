package com.baedalondo.api.guest.repository;

import com.baedalondo.api.guest.domain.GuestRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestRegionRepository extends JpaRepository<GuestRegion, Long> {

    List<GuestRegion> findBySidoNameAndActiveTrue(String sidoName);

    List<GuestRegion> findBySidoNameAndSigunguNameAndActiveTrue(String sidoName, String sigunguName);

    boolean existsBySidoNameAndSigunguName(String sidoName, String sigunguName);
}
