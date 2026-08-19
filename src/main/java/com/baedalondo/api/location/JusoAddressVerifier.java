package com.baedalondo.api.location;

import com.baedalondo.api.location.client.JusoAddressSearchClient;
import com.baedalondo.api.location.dto.JusoAddressRequest;
import com.baedalondo.api.store.dto.StoreEditRequest;
import com.baedalondo.api.store.dto.StoreRegisterRequest;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class JusoAddressVerifier {

    private final JusoAddressSearchClient jusoAddressSearchClient;

    public JusoAddressVerifier(
            JusoAddressSearchClient jusoAddressSearchClient
    ) {
        this.jusoAddressSearchClient = jusoAddressSearchClient;
    }

    public JusoAddressRequest storeRegisterCheckAddress(
            StoreRegisterRequest request
    ) {
        if (request == null || request.getJusoAddress() == null) {
            throw new IllegalArgumentException("주소 정보가 없습니다.");
        }

        return verify(request.getJusoAddress());
    }

    public JusoAddressRequest storeEditCheckAddress(
            StoreEditRequest request
    ) {
        if (request == null || request.getJusoAddress() == null) {
            throw new IllegalArgumentException("주소 정보가 없습니다.");
        }

        return verify(request.getJusoAddress());
    }

    private JusoAddressRequest verify(
            JusoAddressRequest requestedAddress
    ) {

        if (isBlank(requestedAddress.getRoadAddrPart1())) {
            throw new IllegalArgumentException("도로명주소가 없습니다.");
        }

        // 클라이언트가 보내온 주소 문자열을 이용해
        // 행안부 주소검색 API에서 주소를 다시 조회한다.
        JusoAddressRequest verifiedAddress =
                jusoAddressSearchClient.searchFirst(
                        requestedAddress.getRoadAddrPart1()
                );

        // 클라이언트가 보낸 주소 식별정보와
        // 행안부에서 다시 받은 주소 식별정보가 같은지 확인한다.
        if (!isSameAddress(requestedAddress, verifiedAddress)) {
            throw new IllegalArgumentException(
                    "주소 정보가 올바르지 않습니다. 주소를 다시 선택해 주세요."
            );
        }

        /*
         * 상세주소는 행안부에서 정해주는 값이 아니라
         * 사용자가 직접 입력하는 값이므로 원래 요청값을 사용한다.
         *
         * 그 외 주소 정보는 verifiedAddress,
         * 즉 서버가 행안부에서 다시 받아온 값을 사용한다.
         */
        verifiedAddress.setAddrDetail(
                requestedAddress.getAddrDetail()
        );

        return verifiedAddress;
    }

    private boolean isSameAddress(
            JusoAddressRequest requested,
            JusoAddressRequest verified
    ) {

        return Objects.equals(
                requested.getAdmCd(),
                verified.getAdmCd()
        )
                && Objects.equals(
                requested.getRnMgtSn(),
                verified.getRnMgtSn()
        )
                && Objects.equals(
                requested.getUdrtYn(),
                verified.getUdrtYn()
        )
                && Objects.equals(
                requested.getBuldMnnm(),
                verified.getBuldMnnm()
        )
                && Objects.equals(
                requested.getBuldSlno(),
                verified.getBuldSlno()
        )
                && Objects.equals(
                requested.getBdMgtSn(),
                verified.getBdMgtSn()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}