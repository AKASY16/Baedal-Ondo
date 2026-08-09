package com.baedalondo.api.store.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 businessType이 ordinal 숫자가 아니라 Enum 이름 문자열로 저장되는지 확인한다.
 **/
@DataJpaTest
class StoreBusinessTypePersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("business_type 컬럼에 CHICKEN 문자열이 저장된다")
    void savesChickenAsString() {
        assertStoredColumnValue(BusinessType.CHICKEN, "CHICKEN");
    }

    @Test
    @DisplayName("business_type 컬럼에 CAFE_BEVERAGE 문자열이 저장된다")
    void savesCafeBeverageAsString() {
        assertStoredColumnValue(BusinessType.CAFE_BEVERAGE, "CAFE_BEVERAGE");
    }

    @Test
    @DisplayName("business_type 컬럼에 BAKERY 문자열이 저장된다")
    void savesBakeryAsString() {
        assertStoredColumnValue(BusinessType.BAKERY, "BAKERY");
    }

    @Test
    @DisplayName("저장한 업종을 다시 읽으면 같은 BusinessType으로 돌아온다")
    void readsBackAsEnum() {

        Long storeId = entityManager.persistAndGetId(store(BusinessType.BUNSIK), Long.class);
        entityManager.flush();
        entityManager.clear();

        Store found = entityManager.find(Store.class, storeId);

        assertEquals(BusinessType.BUNSIK, found.getBusinessType());
        assertEquals("분식", found.getBusinessType().getDisplayName());
    }

    private void assertStoredColumnValue(BusinessType businessType, String expectedColumnValue) {

        Long storeId = entityManager.persistAndGetId(store(businessType), Long.class);
        entityManager.flush();

        Object stored = entityManager.getEntityManager()
                .createNativeQuery("select business_type from store where id = :id")
                .setParameter("id", storeId)
                .getSingleResult();

        assertEquals(expectedColumnValue, stored);
    }

    private Store store(BusinessType businessType) {
        return new Store(
                "온도식당",
                businessType,
                null, null, null, null, null,
                null, null, null,
                null, null, null,
                null, null, null, null,
                60, 127
        );
    }
}
