<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';



const route = useRoute();
const router = useRouter();

// 탭 정의
const tabs = [
  { id: 'terms', label: '이용약관' },
  { id: 'privacy', label: '개인정보처리방침' },
  { id: 'refund', label: '결제 및 환불 정책' },
  { id: 'host', label: '숙소 등록 운영정책' }
];

// 현재 활성 탭
const activeTab = ref('terms');

// URL 쿼리 파라미터로 탭 설정
onMounted(() => {
  const tab = route.query.tab;
  if (tab && tabs.find(t => t.id === tab)) {
    activeTab.value = tab;
  }
});

// 탭 변경 시 URL 업데이트
const changeTab = (tabId) => {
  activeTab.value = tabId;
  router.replace({ query: { tab: tabId } });
};

// URL 변경 감지
watch(() => route.query.tab, (newTab) => {
  if (newTab && tabs.find(t => t.id === newTab)) {
    activeTab.value = newTab;
  }
});

// 정책 내용
const policyContents = {
  terms: {
    title: '서비스 이용약관',
    effectiveDate: '2025년 12월 23일',
    sections: [
      {
        title: '제1조 (목적)',
        content: '이 약관은 지금이곳(이하 "회사")이 제공하는 숙박 예약 서비스(이하 "서비스")의 이용과 관련하여 회사와 이용자 간의 권리, 의무 및 책임사항, 기타 필요한 사항을 규정함을 목적으로 합니다.'
      },
      {
        title: '제2조 (정의)',
        content: '① "서비스"란 회사가 제공하는 숙박 시설 검색, 예약, 결제 및 관련 부가 서비스를 말합니다.\n② "이용자"란 이 약관에 따라 회사가 제공하는 서비스를 이용하는 회원 및 비회원을 말합니다.\n③ "회원"이란 회사와 서비스 이용계약을 체결하고 이용자 아이디(ID)를 부여받은 자를 말합니다.'
      },
      {
        title: '제3조 (약관의 효력 및 변경)',
        content: '① 이 약관은 서비스 화면에 게시하거나 기타의 방법으로 이용자에게 공지함으로써 효력이 발생합니다.\n② 회사는 필요한 경우 관련 법령을 위배하지 않는 범위에서 이 약관을 변경할 수 있으며, 변경된 약관은 공지사항을 통해 공지합니다.'
      },
      {
        title: '제4조 (서비스의 제공)',
        content: '① 회사는 다음과 같은 서비스를 제공합니다.\n  1. 숙박 시설 정보 제공 및 검색 서비스\n  2. 숙박 예약 서비스\n  3. 결제 서비스\n  4. 고객 상담 서비스\n  5. 기타 회사가 정하는 서비스'
      },
      {
        title: '제5조 (서비스 이용)',
        content: '① 서비스 이용은 회사의 서비스 사용 승낙 직후부터 가능합니다.\n② 서비스 이용시간은 회사의 업무상 또는 기술상 불가능한 경우를 제외하고는 연중무휴 1일 24시간을 원칙으로 합니다.'
      },
      {
        title: '제6조 (회원가입)',
        content: '① 이용자는 회사가 정한 가입 양식에 따라 회원정보를 기입한 후 이 약관에 동의한다는 의사표시를 함으로써 회원가입을 신청합니다.\n② 회사는 제1항과 같이 회원가입을 신청한 이용자 중 다음 각 호에 해당하지 않는 한 회원으로 등록합니다.'
      }
    ]
  },
  privacy: {
    title: '개인정보처리방침',
    effectiveDate: '2025년 12월 23일',
    sections: [
      {
        title: '1. 개인정보의 수집 및 이용 목적',
        content: '지금이곳은 다음의 목적을 위하여 개인정보를 처리합니다. 처리하고 있는 개인정보는 다음의 목적 이외의 용도로는 이용되지 않으며, 이용 목적이 변경되는 경우에는 「개인정보 보호법」 제18조에 따라 별도의 동의를 받는 등 필요한 조치를 이행할 예정입니다.\n\n① 회원 가입 및 관리\n② 서비스 제공 및 요금 정산\n③ 마케팅 및 광고에의 활용'
      },
      {
        title: '2. 수집하는 개인정보의 항목',
        content: '① 필수 수집 항목: 이름, 이메일 주소, 비밀번호, 휴대폰 번호\n② 선택 수집 항목: 프로필 사진, 생년월일, 성별\n③ 서비스 이용 과정에서 자동으로 수집되는 정보: 접속 IP, 쿠키, 서비스 이용 기록, 접속 로그'
      },
      {
        title: '3. 개인정보의 보유 및 이용 기간',
        content: '① 회원 탈퇴 시까지 (단, 관계 법령에 따라 보존 필요가 있는 경우 해당 기간 동안 보관)\n② 전자상거래 등에서의 소비자 보호에 관한 법률에 따른 보존 기간\n  - 계약 또는 청약철회 등에 관한 기록: 5년\n  - 대금결제 및 재화 등의 공급에 관한 기록: 5년\n  - 소비자의 불만 또는 분쟁처리에 관한 기록: 3년'
      },
      {
        title: '4. 개인정보의 제3자 제공',
        content: 'Travel은 원칙적으로 이용자의 개인정보를 외부에 제공하지 않습니다. 다만, 아래의 경우에는 예외로 합니다.\n\n① 이용자가 사전에 동의한 경우\n② 법령의 규정에 의거하거나, 수사 목적으로 법령에 정해진 절차와 방법에 따라 수사기관의 요구가 있는 경우'
      },
      {
        title: '5. 개인정보의 파기',
        content: '① Travel은 개인정보 보유기간의 경과, 처리목적 달성 등 개인정보가 불필요하게 되었을 때에는 지체 없이 해당 개인정보를 파기합니다.\n② 전자적 파일 형태의 정보는 기록을 재생할 수 없는 기술적 방법을 사용합니다.\n③ 종이에 출력된 개인정보는 분쇄기로 분쇄하거나 소각을 통하여 파기합니다.'
      },
      {
        title: '6. 이용자의 권리와 그 행사방법',
        content: '① 이용자는 언제든지 등록되어 있는 자신의 개인정보를 조회하거나 수정할 수 있습니다.\n② 이용자는 언제든지 개인정보 처리의 동의를 철회할 수 있습니다.\n③ 개인정보 열람, 정정·삭제, 처리정지 요구는 고객센터를 통해 신청할 수 있습니다.'
      }
    ]
  },
  refund: {
    title: '결제 및 환불 정책',
    effectiveDate: '2025년 12월 23일',
    sections: [
      {
        title: '1. 결제 정책에 대해 알아두어야 할 사항',
        content: '①결제는 숙소 현지 시간 기준으로 처리 및 기록됩니다.\n②결제 승인 완료 시 예약이 확정되며, 결제 미완료 상태에서는 예약이 확정되지 않을 수 있습니다.\n③결제 금액에는 숙소 요금 및 세금(해당되는 경우)이 포함될 수 있으며, 표시되는 최종 결제 금액은 결제 단계에서 확인할 수 있습니다.'
      },
      {
        title: '2. 결제 진행 방식',
        content: '①이용자는 숙소/객실, 일정, 인원 등을 선택하고 결제를 진행합니다.\n②결제 승인 완료 시 예약이 확정되며, 예약 내역에서 결제 상태를 확인할 수 있습니다.\n③결제 실패/취소/오류 발생 시 예약이 자동으로 확정되지 않거나, 결제 재시도가 필요할 수 있습니다.'
      },
      {
        title: '3. 결제 오류 및 중복 결제',
        content: '①결제는 카드사/PG사 네트워크 상태에 따라 지연되거나 실패할 수 있습니다.\n②동일 예약에 대해 중복 승인/중복 결제로 의심되는 경우, 고객센터로 문의하면 결제 상태 확인 및 조치를 안내합니다.'
      },
      {
        title: '4. 환불 정책에 대해 알아두어야 할 사항',
        content: '①환불 기준은 취소 요청이 접수된 시점(플랫폼 취소 완료 시각 또는 고객센터 접수 시각)을 기준으로 산정합니다.\n②“체크인 n일 전”은 체크인일 00:00(숙소 현지 시간)을 기준으로 역산합니다.\n③환불은 원칙적으로 결제에 사용한 수단으로 환불됩니다(결제수단/카드사 정책에 따라 반영 시점은 달라질 수 있습니다).'
      },
      {
        title: '환불 기준',
        content: '① 체크인 7일 전 취소: 결제 금액 100% 환불\n② 체크인 5~6일 전 취소: 결제 금액의 90% 환불\n③ 체크인 3~4일 전 취소: 결제 금액의 70% 환불\n④ 체크인 1~2일 전 취소: 결제 금액의 50% 환불\n⑤ 체크인 당일 취소 또는 노쇼(No-show): 환불 불가'
      },
      {
        title: '5. 환불 처리 기간',
        content: '① 취소 신청 후 영업일 기준 3~5일 이내에 환불 처리됩니다.\n② 신용카드 결제의 경우, 카드사 정책에 따라 환불 완료까지 추가 시간이 소요될 수 있습니다.\n③ 계좌이체의 경우, 환불 신청 시 등록한 계좌로 입금됩니다.'
      },
      {
        title: '6. 환불 불가 사항',
        content: '① 예약자의 단순 변심에 의한 체크인 당일 취소\n② 숙소 이용 후 제기되는 환불 요청\n③ 천재지변, 전염병 등 불가항력적 상황으로 인한 취소 (숙소 정책에 따름)\n④ 프로모션 또는 특가 상품의 경우 별도 취소 정책 적용'
      },
      {
        title: '7. 예외 상황',
        content: '다음의 경우에는 전액 환불이 가능합니다.\n\n① 숙소 측의 귀책 사유로 예약이 취소된 경우\n② 예약한 객실과 실제 제공된 객실이 현저히 다른 경우\n③ 숙소 시설에 심각한 하자가 있어 정상적인 이용이 불가능한 경우'
      },
      {
        title: '8. 환불 신청 방법',
        content: '① 마이페이지 > 예약 내역에서 취소 신청\n② 고객센터 전화 문의 (평일 10:00 ~ 19:00)\n③ 1:1 문의를 통한 취소 요청'
      }
    ]
  },
  host: {
    title: '숙소 및 객실 등록 운영정책',
    effectiveDate: '2025년 12월 23일',
    sections: [
      {
        title: '1. 숙소 정보 등록 기준',
        content: '① 호스트는 숙소의 실제 위치, 연락처, 시설 정보 등을 정확하게 입력해야 합니다.\n② 허위 입력을 하거나 과장된 정보를 제공하여 발생한 모든 문제의 책임은 호스트에게 있습니다.\n③ 숙소 이름에 특수문자나 이모지, 광고성 문구를 포함할 수 없으며, 관리자가 이를 수정하거나 삭제할 수 있습니다.'
      },
      {
        title: '2. 객실 정보 등록 기준',
        content: '① 객실 요금은 세금 및 봉사료를 포함한 최종 금액으로 설정해야 합니다.\n② 기준 인원과 최대 인원을 명확히 표기해야 하며, 인원 추가 요금에 대한 기준도 명시해야 합니다.\n③ 객실 이미지는 실제 투숙할 객실의 모습을 반영해야 하며, 과도한 보정이나 관련 없는 이미지는 사용할 수 없습니다.'
      },
      {
        title: '3. 금지된 콘텐츠 및 행위',
        content: '다음과 같은 콘텐츠를 등록하거나 행위를 할 경우, 경고 없이 숙소 등록이 취소되거나 계정이 정지될 수 있습니다.\n\n① 성매매, 도박 등 불법 행위를 조장하는 내용\n② 타인의 저작권, 상표권 등 지적재산권을 침해하는 이미지나 텍스트\n③ 욕설, 비방, 차별적 발언 등 커뮤니티 가이드라인을 위반하는 내용\n④ 고의적인 예약 취소 유도 및 플랫폼 외부 거래 유도'
      },
      {
        title: '4. 등록 승인 및 거절',
        content: '① 등록 신청된 숙소는 플랫폼 운영팀의 검수를 거쳐 승인 후 공개됩니다.\n② 사진 품질이 너무 낮거나(흐림, 깨짐 등), 필수 정보가 누락된 경우 승인이 거절될 수 있습니다.\n③ 관련 법령(공중위생관리법 등)에 위배되는 시설로 판단될 경우 등록이 제한됩니다.'
      },
      {
        title: '5. 운영 중단 및 페널티',
        content: '① 호스트가 정당한 사유 없이 확정된 예약을 일방적으로 취소할 경우 페널티가 부과될 수 있습니다.\n② 실제 숙소 상태가 등록된 정보와 현저히 달라 고객 컴플레인이 반복될 경우, 노출 중단 또는 서비스 이용 제한 조치가 취해질 수 있습니다.'
      }
    ]
  }
};

// 현재 탭의 콘텐츠
const currentContent = computed(() => policyContents[activeTab.value]);
</script>

<template>
  <div class="policy-page">
    <!-- 헤더 -->
    <div class="policy-header">
      <h1 class="policy-title">약관 및 정책</h1>
    </div>

    <!-- 탭 네비게이션 -->
    <div class="tab-navigation">
      <div class="tab-container">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          :class="['tab-button', { active: activeTab === tab.id }]"
          @click="changeTab(tab.id)"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- 콘텐츠 영역 -->
    <div class="policy-content">
      <div class="content-wrapper">
        <!-- 정책 제목 및 시행일 -->
        <div class="content-header">
          <h2 class="content-title">{{ currentContent.title }}</h2>
          <p class="effective-date">시행일자: {{ currentContent.effectiveDate }}</p>
        </div>

        <!-- 정책 섹션들 -->
        <div class="sections">
          <div 
            v-for="(section, index) in currentContent.sections" 
            :key="index"
            class="section"
          >
            <h3 class="section-title">{{ section.title }}</h3>
            <p class="section-content">{{ section.content }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.policy-page {
  min-height: 100vh;
  background-color: #f8f9fa;
}

/* 헤더 */
.policy-header {
  background: linear-gradient(135deg, #6DC3BB 0%, #5ab3aa 100%);
  padding: 40px 24px;
  text-align: center;
}

.policy-title {
  color: white;
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  font-family: 'Noto Sans KR', sans-serif;
}

/* 탭 네비게이션 */
.tab-navigation {
  background: white;
  border-bottom: 1px solid #e5e5e5;
  position: sticky;
  top: 70px; /* 메인 헤더 높이에 맞춤 */
  z-index: 50; /* 메인 헤더(z-index: 100)보다 낮게 설정 */
}

.tab-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  padding: 0 24px;
}

.tab-button {
  flex: 1;
  padding: 16px 24px;
  background: none;
  border: none;
  font-size: 15px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  font-family: 'Noto Sans KR', sans-serif;
}

.tab-button:hover {
  color: #6DC3BB;
}

.tab-button.active {
  color: #6DC3BB;
  font-weight: 600;
}

.tab-button.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #6DC3BB;
  border-radius: 3px 3px 0 0;
}

/* 콘텐츠 영역 */
.policy-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
}

.content-wrapper {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 40px;
}

.content-header {
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.content-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px 0;
  font-family: 'Noto Sans KR', sans-serif;
}

.effective-date {
  font-size: 14px;
  color: #888;
  margin: 0;
}

/* 섹션 */
.sections {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.section {
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
  font-family: 'Noto Sans KR', sans-serif;
}

.section-content {
  font-size: 14px;
  color: #555;
  line-height: 1.8;
  margin: 0;
  white-space: pre-line;
  font-family: 'Noto Sans KR', sans-serif;
}

/* 반응형 - 태블릿 */
@media (max-width: 768px) {
  .policy-header {
    padding: 32px 16px;
  }

  .policy-title {
    font-size: 24px;
  }

  .tab-container {
    padding: 0 16px;
  }

  .tab-button {
    padding: 14px 12px;
    font-size: 14px;
  }

  .policy-content {
    padding: 24px 16px;
  }

  .content-wrapper {
    padding: 24px;
    border-radius: 12px;
  }

  .content-title {
    font-size: 20px;
  }

  .section-title {
    font-size: 15px;
  }

  .section-content {
    font-size: 13px;
  }
}

/* 반응형 - 모바일 */
@media (max-width: 480px) {
  .policy-header {
    padding: 24px 12px;
  }

  .policy-title {
    font-size: 20px;
  }

  .tab-container {
    padding: 0 8px;
  }

  .tab-button {
    padding: 12px 8px;
    font-size: 13px;
  }

  .policy-content {
    padding: 16px 12px;
  }

  .content-wrapper {
    padding: 20px 16px;
  }

  .content-title {
    font-size: 18px;
  }

  .sections {
    gap: 20px;
  }

  .section-title {
    font-size: 14px;
  }

  .section-content {
    font-size: 12px;
    line-height: 1.7;
  }
}
</style>
