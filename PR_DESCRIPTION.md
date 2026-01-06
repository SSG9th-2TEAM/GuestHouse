## 💡 PR 제목
fix: [Search] 가격 필터 적용 시 페이지네이션 카운트 오류 수정 및 쿼리 최적화

---

## 🔗 관련 이슈
> 진행 중이면 `Refs #123`, 완전 해결이면 `Closes #123` (완전히 끝났을 때만)
- Closes: # (이슈 번호 없음)

---

## 📌 작업 내용 요약
- [x] `searchPublicListNoDates` 메서드의 `countQuery`에 누락된 가격 필터(minPrice, maxPrice) 추가
- [x] `searchPublicListNoDates`, `searchPublicListByThemeNoDates`, `searchPublicListByBoundsNoDates`, `searchPublicListByThemeAndBoundsNoDates` 쿼리를 CTE(`priced_accommodations`)를 사용하여 리팩토링 (중복된 가격 계산 로직 제거 및 유지보수성 향상)
- [x] `searchPublicList` 메서드의 `countQuery`에 누락된 가격 필터 및 CTE(`min_prices`) 추가 (High Severity 버그 수정)
- [x] `SearchRepository.java`의 컴파일 오류(미완료된 문자열 리터럴, BOM 문자) 수정
- [x] 가격 필터링 동작 검증을 위한 통합 테스트 케이스 추가

---

## 🧱 변경 범위 (Scope)
### Backend
- [ ] API 추가/수정
- [ ] Validation/예외처리
- [ ] 인증/인가
- [x] DB 스키마/쿼리 (`SearchRepository` Native Query 수정)
- [ ] 기타:

### Frontend
- [ ] UI/라우팅
- [ ] 상태관리/비동기 로직
- [ ] 입력값/검증
- [ ] 기타:

---

## 🧪 테스트 내역
### Backend
- [x] 로컬에서 애플리케이션 실행 확인
- [x] 단위 테스트 통과 (`SearchServiceIntegrationTest`)
- [ ] API 수동 테스트 (Postman / Swagger 등)

### Frontend
- [ ] `npm run dev` 로컬 실행 확인
- [ ] 주요 화면/기능 수동 테스트

### 테스트 상세(필수)
> 테스트한 API/페이지/케이스를 간단히 적어주세요.
- **SearchServiceIntegrationTest**:
    - `searchByPriceFiltersWithoutDates`: 날짜 없이 가격 범위로 검색 시 결과 및 카운트 검증 (Pass)
    - `searchWithDatesAndPriceFiltersReturnsCorrectCount`: 날짜와 가격 범위를 함께 사용하여 검색 시 페이지네이션 카운트 검증 (Pass)
    - `searchPublicListByThemeAndBoundsNoDates_returnsCorrectResults`: 테마, 위치 경계, 가격 필터를 모두 적용한 복합 조건 검색 결과 검증 (Pass)

---

## 🖼️ 화면 캡처 / 시연 영상 (Frontend 변경 시 권장)
- before: (없음)
- after: (없음)

---

## ⚠️ 영향도 / 리스크 / 롤백
> 리뷰어가 “머지해도 되는지” 판단하기 위한 섹션입니다.
- **영향도**: 검색 목록 조회 API의 페이지네이션 `totalElements` 값이 정확하게 계산되어 반환됩니다. 기존 API 포맷 변경은 없습니다.
- **리스크**: 복잡한 Native SQL 쿼리가 수정되었으나, 관련된 통합 테스트를 추가하여 정합성을 검증했습니다.
- **롤백 방법**: `revert` 커밋

---

## ✅ 리뷰어 체크 포인트 (선택)
> 리뷰어가 집중해서 봐줬으면 하는 부분이 있으면 적어주세요.
- `SearchRepository` 내의 `searchPublicListNoDates`와 `searchPublicList` 쿼리에서 CTE(`priced_accommodations`, `min_prices`)를 활용하여 가격 필터링 로직을 구성한 부분이 적절한지 확인 부탁드립니다.
- `searchPublicListByThemeAndBoundsNoDates` 메서드의 CTE 적용 부분이 의도대로 구현되었는지 확인 부탁드립니다.
- `effective_price` 계산 로직이 기존 비즈니스 로직(인원 수에 따른 최저가 적용 등)과 일치하는지 확인해 주세요.
