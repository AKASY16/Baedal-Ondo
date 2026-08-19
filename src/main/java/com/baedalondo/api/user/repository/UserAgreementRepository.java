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

    /** 회원 탈퇴 시 동의 이력을 지운다. 반환값은 지운 행 수다. */
    int deleteByUserAccountId(Long userAccountId);
}
