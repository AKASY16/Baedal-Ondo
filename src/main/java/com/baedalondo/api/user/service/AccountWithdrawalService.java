package com.baedalondo.api.user.service;

import com.baedalondo.api.store.repository.StoreRepository;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.repository.UserAccountRepository;
import com.baedalondo.api.user.repository.UserAgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 회원 탈퇴.

 상태만 WITHDRAWN으로 바꾸지 않고 실제로 지운다.
 개인정보처리방침이 회원 계정과 동의 이력은 "회원 탈퇴 시까지", 가게 정보는
 "가게 삭제 또는 회원 탈퇴 시까지" 보유한다고 밝히고, 보유 기간이 지나면
 지체 없이 파기한다고 적혀 있다. 상태 플래그만 바꾸면 그 약속과 어긋난다.

 이 서비스는 결제나 거래가 없어 전자상거래법상 보존 의무가 걸리는 기록도 없다.
 */
@Service
public class AccountWithdrawalService {

    private final UserAccountRepository userAccountRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountWithdrawalService(UserAccountRepository userAccountRepository,
                                    UserAgreementRepository userAgreementRepository,
                                    StoreRepository storeRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.userAgreementRepository = userAgreementRepository;
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     비밀번호를 다시 확인한 뒤 계정과 딸린 데이터를 지운다.

     되돌릴 수 없는 동작이라 세션만으로 실행하지 않는다. 자리를 비운 사이 남의 손이
     닿거나 세션이 탈취된 경우를 막는다.

     삭제 순서는 외래키를 따른다. store와 user_agreement가 user_account를 참조하므로
     자식을 먼저 지운다.
     */
    @Transactional
    public void withdraw(Long userId, String rawPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

        if (rawPassword == null || rawPassword.isBlank()
                || !passwordEncoder.matches(rawPassword, account.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }

        int deletedStores = storeRepository.deleteByUserId(userId);
        int deletedAgreements = userAgreementRepository.deleteByUserAccountId(userId);
        userAccountRepository.delete(account);

        // 로그인 ID나 이메일은 남기지 않는다. 무엇이 지워졌는지만 센다.
        log.info("회원 탈퇴 처리 완료. userId={}, 가게={}건, 동의이력={}건",
                userId, deletedStores, deletedAgreements);
    }

    private static final Logger log = LoggerFactory.getLogger(AccountWithdrawalService.class);
}
