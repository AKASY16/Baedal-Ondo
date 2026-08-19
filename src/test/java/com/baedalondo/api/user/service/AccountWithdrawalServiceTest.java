package com.baedalondo.api.user.service;

import com.baedalondo.api.store.domain.BusinessType;
import com.baedalondo.api.store.domain.Store;
import com.baedalondo.api.store.repository.StoreRepository;
import com.baedalondo.api.support.MySqlTestSupport;
import com.baedalondo.api.user.domain.AgreementType;
import com.baedalondo.api.user.domain.UserAccount;
import com.baedalondo.api.user.domain.UserAgreement;
import com.baedalondo.api.user.repository.UserAccountRepository;
import com.baedalondo.api.user.repository.UserAgreementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 회원 탈퇴가 실제로 데이터를 지우는지 확인한다.

 상태 플래그만 바꾸면 개인정보처리방침이 약속한 "지체 없이 파기"와 어긋난다.
 그리고 store와 user_agreement가 user_account를 참조하므로 삭제 순서가 틀리면
 외래키 제약에 걸린다. 실제 MySQL에서 제약이 걸린 채로 확인해야 의미가 있다.
 **/
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountWithdrawalServiceTest extends MySqlTestSupport {

    private static final String RAW_PASSWORD = "Test1234!";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserAgreementRepository userAgreementRepository;

    @Autowired
    private StoreRepository storeRepository;

    private AccountWithdrawalService accountWithdrawalService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        accountWithdrawalService = new AccountWithdrawalService(
                userAccountRepository,
                userAgreementRepository,
                storeRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("탈퇴하면 계정과 가게, 동의 이력이 모두 지워진다")
    void deletesAccountWithOwnedData() {
        Long userId = createAccountWithData();

        accountWithdrawalService.withdraw(userId, RAW_PASSWORD);
        entityManager.flush();
        entityManager.clear();

        assertTrue(userAccountRepository.findById(userId).isEmpty(), "계정이 남아 있다");
        assertEquals(0, storeRepository.findByUserIdOrderByIdAsc(userId).size(), "가게가 남아 있다");
        assertEquals(0, countAgreements(userId), "동의 이력이 남아 있다");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 아무것도 지우지 않는다")
    void keepsEverythingWhenPasswordIsWrong() {
        // 세션만으로 실행되면 자리를 비운 사이 남의 손이 닿거나 세션이 탈취됐을 때
        // 되돌릴 수 없는 삭제가 그대로 일어난다.
        Long userId = createAccountWithData();

        assertThrows(IllegalArgumentException.class,
                () -> accountWithdrawalService.withdraw(userId, "WrongPassword1!"));

        entityManager.clear();

        assertTrue(userAccountRepository.findById(userId).isPresent());
        assertEquals(2, storeRepository.findByUserIdOrderByIdAsc(userId).size());
        assertEquals(2, countAgreements(userId));
    }

    @Test
    @DisplayName("비밀번호가 비어 있으면 거부한다")
    void rejectsBlankPassword() {
        Long userId = createAccountWithData();

        assertThrows(IllegalArgumentException.class,
                () -> accountWithdrawalService.withdraw(userId, ""));
        assertThrows(IllegalArgumentException.class,
                () -> accountWithdrawalService.withdraw(userId, null));

        entityManager.clear();
        assertTrue(userAccountRepository.findById(userId).isPresent());
    }

    @Test
    @DisplayName("로그인하지 않았거나 없는 계정이면 거부한다")
    void rejectsUnknownAccount() {
        assertThrows(IllegalArgumentException.class,
                () -> accountWithdrawalService.withdraw(null, RAW_PASSWORD));
        assertThrows(IllegalArgumentException.class,
                () -> accountWithdrawalService.withdraw(-1L, RAW_PASSWORD));
    }

    @Test
    @DisplayName("다른 회원의 가게는 건드리지 않는다")
    void keepsOtherMembersData() {
        Long userId = createAccountWithData();
        Long otherUserId = createAccountWithData("other-user", "other@example.com");

        accountWithdrawalService.withdraw(userId, RAW_PASSWORD);
        entityManager.flush();
        entityManager.clear();

        assertTrue(userAccountRepository.findById(otherUserId).isPresent());
        assertEquals(2, storeRepository.findByUserIdOrderByIdAsc(otherUserId).size());
        assertEquals(2, countAgreements(otherUserId));
    }

    @Test
    @DisplayName("탈퇴한 로그인 ID는 다시 쓸 수 있다")
    void freesLoginIdAfterWithdrawal() {
        // 상태만 바꾸면 유니크 제약 때문에 같은 ID로 다시 가입할 수 없다.
        Long userId = createAccountWithData();

        accountWithdrawalService.withdraw(userId, RAW_PASSWORD);
        entityManager.flush();
        entityManager.clear();

        assertFalse(userAccountRepository.existsByLoginId("withdraw-test"));

        Long reJoined = createAccountWithData();
        assertTrue(userAccountRepository.findById(reJoined).isPresent());
    }

    private long countAgreements(Long userId) {
        return userAgreementRepository.findAll().stream()
                .filter(agreement -> agreement.getUserAccount() != null)
                .filter(agreement -> userId.equals(agreement.getUserAccount().getId()))
                .count();
    }

    private Long createAccountWithData() {
        return createAccountWithData("withdraw-test", "withdraw@example.com");
    }

    private Long createAccountWithData(String loginId, String email) {
        UserAccount account = new UserAccount(
                loginId, email, passwordEncoder.encode(RAW_PASSWORD), "ROLE_USER");
        entityManager.persist(account);

        entityManager.persist(store(account, "탈퇴검증 치킨", BusinessType.CHICKEN, 60, 127));
        entityManager.persist(store(account, "탈퇴검증 카페", BusinessType.CAFE_BEVERAGE, 61, 126));

        entityManager.persist(UserAgreement.record(
                account, AgreementType.TERMS_OF_SERVICE, "v1", LocalDateTime.now()));
        entityManager.persist(UserAgreement.record(
                account, AgreementType.MARKETING_EMAIL, "v1", LocalDateTime.now()));

        entityManager.flush();
        entityManager.clear();

        return account.getId();
    }

    private Store store(UserAccount owner, String name, BusinessType businessType, int nx, int ny) {
        Store store = new Store(
                name,
                businessType,
                null, null, null, null, null,
                "서울", null, null,
                null, null, null,
                null, null, null, null,
                nx, ny
        );
        store.setUser(owner);

        return store;
    }
}
