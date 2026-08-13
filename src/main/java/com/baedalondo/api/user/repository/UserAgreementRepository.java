package com.baedalondo.api.user.repository;

import com.baedalondo.api.user.domain.AgreementType;
import com.baedalondo.api.user.domain.UserAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAgreementRepository extends JpaRepository<UserAgreement, Long> {

    List<UserAgreement> findAllByUserAccountIdAndAgreementTypeOrderByAgreedAtDesc(
            Long userAccountId,
            AgreementType agreementType
    );
}
