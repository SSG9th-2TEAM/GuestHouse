import{_ as se,r as t,b as oe,s as le,X as te,k as ne,q as I,c as n,e,d as f,t as d,z as g,F as y,C as D,M as P,f as E,A as Y,n as S,Y as F,o as r}from"./index-DunUV3Ya.js";import{f as re,b as ae}from"./theme-CH8NxBMC.js";const ie={class:"register-page"},ce={class:"register-container"},de={class:"page-header"},ue={class:"progress-steps"},ve={class:"step-labels"},be={class:"terms-section"},me={class:"all-agree"},ge={class:"term-list"},he=["onUpdate:modelValue"],pe=["onClick"],ke=["disabled"],fe={class:"theme-section"},ye={key:0,class:"theme-loading"},_e={key:1,class:"theme-error"},we={key:2,class:"theme-categories"},Ce={class:"category-header"},Te={class:"category-emoji"},xe={class:"category-name"},Ae={class:"theme-chips"},Me=["onClick"],je=["src","alt"],Ie={class:"chip-label"},Ee={key:0,class:"chip-check"},Se={class:"final-actions"},Le=["disabled"],Ne=["disabled"],Ve={class:"modal-content"},qe={class:"modal-message"},Be={class:"modal-content large"},Ue={class:"terms-content-scroll"},Re=["innerHTML"],L=3,De={__name:"SocialSignupView",setup(Pe){const _=ne(),N=le(),c=t(1),v=t(!1);oe(async()=>{const o=N.query.accessToken,s=N.query.refreshToken;o&&s?(te(o,s),console.log("소셜 로그인 토큰 저장 완료"),await G()):(console.error("토큰이 없습니다. 로그인 페이지로 이동합니다."),_.push("/login"))});const w=t(!1),b=t([{id:1,label:"(필수) 서비스 이용약관",required:!0,checked:!1,content:`
    제1조 (목적)<br/>
    본 약관은 [회사명]이 제공하는 게스트하우스 예약 플랫폼 서비스(이하 "서비스")의 이용과 관련하여 회사와 회원 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.<br/><br/>

    제2조 (서비스의 내용)<br/>
    서비스는 게스트하우스 정보 검색, 예약 및 결제, 후기 작성, 마이페이지를 통한 예약 관리 등의 기능을 제공합니다. 회사는 통신판매중개자로서, 실제 숙박 서비스는 호스트(숙소 운영자)가 제공합니다.<br/><br/>

    제3조 (회원의 의무)<br/>
    1. 회원은 본 약관 및 관계 법령을 준수해야 합니다.<br/>
    2. 회원은 정확한 정보를 제공해야 하며, 허위 정보로 인해 발생하는 불이익에 대한 책임은 회원에게 있습니다.<br/>
    3. 회원은 서비스 이용과 관련하여 다음 행위를 하여서는 안 됩니다:<br/>
       - 허위 정보 등록<br/>
       - 타인의 정보 도용<br/>
       - 서비스의 정상적인 운영 방해<br/>
       - 회사의 지식재산권 침해<br/>
       - 기타 불법적이거나 부당한 행위<br/><br/>

    제4조 (예약 및 결제)<br/>
    1. 회원은 서비스를 통해 게스트하우스 예약을 요청할 수 있으며, 회사는 호스트를 대신하여 예약 접수 및 결제를 중개합니다.<br/>
    2. 예약 확정 및 결제 완료 시, 회원과 호스트 간에 숙박 계약이 체결됩니다.<br/>
    3. 예약 취소 및 환불 정책은 각 숙소의 정책 또는 회사의 표준 정책에 따르며, 회원은 이를 확인 후 예약해야 합니다.<br/><br/>

    제5조 (후기 작성 및 관리)<br/>
    1. 회원은 숙박 서비스 이용 후 숙소에 대한 후기(리뷰)를 작성할 수 있습니다.<br/>
    2. 후기 내용은 객관적이고 사실에 기반해야 하며, 욕설, 비방, 허위 사실 유포 등 타인의 권리를 침해하거나 서비스의 건전한 운영을 저해하는 내용은 삭제될 수 있습니다.<br/><br/>

    제6조 (서비스 이용 제한)<br/>
    회사는 회원이 본 약관의 의무를 위반하거나 서비스의 정상적인 운영을 방해하는 경우, 서비스 이용을 제한하거나 회원 자격을 상실시킬 수 있습니다.<br/><br/>

    제7조 (면책 조항)<br/>
    회사는 천재지변, 불가항력 또는 회원의 귀책사유로 인한 서비스 이용의 장애에 대하여는 책임을 지지 않습니다. 또한, 회사는 통신판매중개자로서 호스트와 회원 간의 직접적인 숙박 계약에 대한 일차적인 책임을 지지 않습니다.<br/><br/>

    (본 약관의 상세 내용은 [회사명] 웹사이트에 게시된 전문을 참고하여 주십시오.)
    `},{id:2,label:"(필수) 개인정보 처리방침",required:!0,checked:!1,content:`
    제1조 (개인정보의 수집 및 이용 목적)<br/>
    [회사명] (이하 "회사")는 게스트하우스 예약 서비스 제공을 위해 다음 목적에 따라 최소한의 개인정보를 수집 및 이용합니다.<br/>
    1. 회원 가입 및 서비스 이용(예약, 결제, 후기 작성 등)<br/>
    2. 불만 처리 등 고객 상담<br/>
    3. 마케팅 및 광고 (선택 동의 시)<br/><br/>

    제2조 (수집하는 개인정보 항목)<br/>
    회사는 원활한 서비스 제공을 위해 다음과 같은 개인정보를 수집할 수 있습니다.<br/>
    1. **회원 가입 시**: 이메일 주소(ID), 비밀번호, 휴대전화 번호, 관심 테마 정보(선택)<br/>
    2. **예약 및 결제 시**: 예약자 이름, 숙박 인원, 연락처, 결제 정보(카드사, 카드번호 일부 등)<br/>
    3. **서비스 이용 시**: IP 주소, 서비스 이용 기록, 접속 로그, 쿠키 등<br/><br/>

    제3조 (개인정보의 보유 및 이용 기간)<br/>
    회원의 개인정보는 원칙적으로 개인정보 수집 및 이용 목적이 달성되면 지체 없이 파기합니다. 단, 관계 법령의 규정에 의하여 보존할 필요가 있는 경우, 회사는 관계 법령에서 정한 일정한 기간 동안 회원정보를 보관합니다.<br/><br/>

    제4조 (개인정보의 제3자 제공)<br/>
    회사는 회원의 개인정보를 "제1조 (개인정보의 수집 및 이용 목적)"에서 고지한 범위 내에서만 사용하며, 회원의 사전 동의 없이 동 범위를 초과하여 이용하거나 원칙적으로 외부에 제공하지 않습니다. 다만, 예약 서비스 제공을 위해 필요한 최소한의 정보(예약자 이름, 연락처, 숙박 인원 등)는 해당 호스트에게 제공될 수 있습니다.<br/><br/>

    제5조 (개인정보의 파기 절차 및 방법)<br/>
    회사는 개인정보 보유기간의 경과, 처리목적 달성 등 개인정보가 불필요하게 되었을 때에는 지체 없이 해당 개인정보를 파기합니다. 전자적 파일 형태의 정보는 기록을 재생할 수 없는 기술적 방법을 사용하며, 종이 문서 형태의 정보는 분쇄기로 분쇄하거나 소각하여 파기합니다.<br/><br/>

    (본 개인정보 처리방침의 상세 내용은 [회사명] 웹사이트에 게시된 전문을 참고하여 주십시오.)
    `},{id:4,label:"(필수) 만 19세 이상 확인",required:!0,checked:!1,content:`
    연령 확인 안내 (필수)<br/><br/>
    본 서비스는 청소년보호법 등 관련 법령에 따라 만 19세 미만 청소년의 숙박 예약 및 이용을 제한하고 있습니다.<br/><br/>
    이에 회원으로 가입하고 서비스를 이용하기 위해서는 본인이 만 19세 이상임을 확인하고 동의해야 합니다.<br/><br/>
    만 19세 미만의 자가 허위의 정보로 가입 및 예약을 진행하여 발생하는 모든 문제에 대한 책임은 회원 본인 및 법정대리인에게 있습니다.
    `},{id:3,label:"(선택) 마케팅 정보 수신 동의",required:!1,checked:!1,content:`
    마케팅 정보 수신 동의 (선택)<br/><br/>

    [회사명]은(는) 회원님께 더 유용하고 맞춤화된 서비스 및 혜택을 제공하기 위해 마케팅 정보를 발송하고자 합니다.<br/><br/>

    **동의하시는 경우, 아래와 같은 정보를 받아보실 수 있습니다.**<br/>
    1. 신규 숙소 및 추천 상품 정보: 회원님의 관심사에 맞는 새로운 게스트하우스 또는 특별 할인 상품 소식<br/>
    2. 이벤트 및 프로모션: 쿠폰, 할인 행사, 시즌별 특별 이벤트 등 회원님께 유리한 혜택 정보<br/>
    3. 서비스 관련 소식: 서비스 업데이트, 새로운 기능 소개 등 서비스 이용에 도움이 되는 정보<br/><br/>

    수신 채널: 이메일, 문자메시지(SMS/MMS), 앱 푸시 알림<br/><br/>

    본 동의는 선택 사항이며, 동의하지 않으셔도 서비스의 기본 기능 이용에는 어떠한 제한도 없습니다. 마케팅 정보 수신 동의 여부는 "마이페이지 > 개인정보 관리"에서 언제든지 변경하거나 철회할 수 있습니다. 동의를 철회하시는 경우에도 법령에 따른 의무 이행을 위한 정보 및 비마케팅성 정보는 계속 발송될 수 있습니다.<br/><br/>

    (본 마케팅 정보 수신 동의에 대한 상세 내용은 [회사명] 웹사이트에 게시된 전문을 참고하여 주십시오.)
    `}]),H=()=>{b.value.forEach(o=>o.checked=w.value)},O=()=>{w.value=b.value.every(o=>o.checked)},V=I(()=>b.value.filter(o=>o.required).every(o=>o.checked)),x=t(!1),q=t(""),B=t(""),$=o=>{q.value=o.label,B.value=o.content,x.value=!0},A=()=>{x.value=!1},h=t([]),p=t(!1),k=t(""),C=t([]),z={NATURE:{emoji:"🌿",name:"자연"},CULTURE:{emoji:"🏛️",name:"문화"},ACTIVITY:{emoji:"🏄",name:"활동"},VIBE:{emoji:"✨",name:"분위기"},PARTY:{emoji:"🥳",name:"파티"},MEETING:{emoji:"💞",name:"만남"},PERSONA:{emoji:"👤",name:"특성/성향"},FACILITY:{emoji:"🏠",name:"시설"},FOOD:{emoji:"🍴",name:"음식"},PLAY:{emoji:"🎮",name:"놀이"}},U=I(()=>{if(C.value.length===0)return z;const o={};return C.value.forEach(s=>{o[s.categoryKey]={emoji:s.emoji,name:s.categoryName}}),o}),X=I(()=>{const o={};return h.value.forEach(s=>{o[s.category]||(o[s.category]=[]),o[s.category].push(s)}),o}),G=async()=>{p.value=!0,k.value="";try{const[o,s]=await Promise.all([re(),ae()]);s.ok&&Array.isArray(s.data)?(C.value=s.data,console.log("카테고리 로드 성공:",C.value)):console.warn("카테고리 목록 로드 실패, 기본값 사용:",s),o.ok&&Array.isArray(o.data)?(h.value=o.data.map(l=>({id:l.id,category:l.themeCategory,label:l.themeName,imageUrl:l.themeImageUrl,selected:!1})),console.log("테마 로드 성공:",h.value.length,"개")):(k.value="테마 목록을 불러오지 못했습니다.",console.error("테마 목록 로드 실패:",o))}catch(o){k.value="테마 목록을 불러오는 중 오류가 발생했습니다.",console.error("테마 목록 로드 에러:",o)}finally{p.value=!1}},J=o=>{if(o.selected){o.selected=!1;return}if(h.value.filter(l=>l.selected).length>=L){u(`테마는 최대 ${L}개까지만 선택할 수 있습니다.`,"info");return}o.selected=!0},M=t(!1),R=t(""),Q=t("info"),T=t(null),u=(o,s="info",l=null)=>{R.value=o,Q.value=s,T.value=l,M.value=!0},j=()=>{M.value=!1,T.value&&(T.value(),T.value=null)},W=()=>{c.value>1?c.value--:_.push("/login")},Z=()=>{c.value===1&&(V.value?c.value=2:u("필수 약관에 동의해주세요.","error"))},K=async()=>{v.value=!0;try{const o=h.value.filter(m=>m.selected).map(m=>m.id),s=b.value.find(m=>m.id===3),l=s?s.checked:!1,a={termsAgreed:!0,themeIds:o.length>0?o:null,marketingAgreed:l};console.log("소셜 회원가입 데이터:",a);const i=await F(a);console.log("소셜 회원가입 응답:",i),i.ok&&i.data?(console.log("소셜 회원가입 완료!"),u("회원가입이 완료되었습니다!","success",()=>_.push("/"))):(console.error("소셜 회원가입 실패:",i),u(`회원가입에 실패했습니다.
잠시 후 다시 시도해주세요.`,"error"))}catch(o){console.error("소셜 회원가입 에러:",o),u("회원가입 중 오류가 발생했습니다.","error")}finally{v.value=!1}},ee=async()=>{v.value=!0;try{const o=b.value.find(i=>i.id===3),l={termsAgreed:!0,themeIds:null,marketingAgreed:o?o.checked:!1};console.log("소셜 회원가입 데이터 (건너뛰기):",l);const a=await F(l);console.log("소셜 회원가입 응답 (건너뛰기):",a),a.ok&&a.data?(console.log("소셜 회원가입 완료 (건너뛰기)!"),u("회원가입이 완료되었습니다!","success",()=>_.push("/"))):(console.error("소셜 회원가입 실패 (건너뛰기):",a),u(`회원가입에 실패했습니다.
잠시 후 다시 시도해주세요.`,"error"))}catch(o){console.error("소셜 회원가입 에러:",o),u("회원가입 중 오류가 발생했습니다.","error")}finally{v.value=!1}};return(o,s)=>(r(),n("div",ie,[e("div",ce,[e("div",de,[e("button",{class:"back-btn",onClick:W},[...s[1]||(s[1]=[e("svg",{xmlns:"http://www.w3.org/2000/svg",width:"24",height:"24",viewBox:"0 0 24 24",fill:"none",stroke:"currentColor","stroke-width":"2","stroke-linecap":"round","stroke-linejoin":"round"},[e("path",{d:"M19 12H5m7 7l-7-7 7-7"})],-1)])]),e("h1",null,d(c.value===1?"약관 동의":"관심 테마 선택"),1)]),e("div",ue,[e("div",{class:g(["step",{active:c.value>=1,done:c.value>1}])},[...s[2]||(s[2]=[e("span",null,"1",-1)])],2),e("div",{class:g(["step-line",{active:c.value>=2}])},null,2),e("div",{class:g(["step",{active:c.value>=2}])},[...s[3]||(s[3]=[e("span",null,"2",-1)])],2)]),e("div",ve,[e("span",{class:g({active:c.value===1})},"약관동의",2),e("span",{class:g({active:c.value===2})},"테마선택",2)]),c.value===1?(r(),n(y,{key:0},[e("div",be,[e("label",me,[D(e("input",{type:"checkbox","onUpdate:modelValue":s[0]||(s[0]=l=>w.value=l),onChange:H},null,544),[[P,w.value]]),s[4]||(s[4]=e("span",null,"전체 동의",-1))]),s[5]||(s[5]=e("hr",{class:"divider"},null,-1)),e("div",ge,[(r(!0),n(y,null,E(b.value,l=>(r(),n("label",{key:l.id,class:"term-row"},[D(e("input",{type:"checkbox","onUpdate:modelValue":a=>l.checked=a,onChange:O},null,40,he),[[P,l.checked]]),e("span",null,d(l.label),1),e("button",{type:"button",class:"view-term-btn",onClick:S(a=>$(l),["stop"])},"보기",8,pe)]))),128))])]),e("button",{class:"next-btn",disabled:!V.value,onClick:Z},"다음",8,ke)],64)):f("",!0),c.value===2?(r(),n(y,{key:1},[e("div",fe,[s[10]||(s[10]=e("h2",{class:"theme-title"},"관심 테마를 선택해주세요",-1)),e("p",{class:"theme-desc"},[s[6]||(s[6]=Y("마음에 드는 여행 스타일을 선택하시면",-1)),s[7]||(s[7]=e("br",null,null,-1)),Y("꼭 맞는 숙소를 추천해 드립니다. (최대 "+d(L)+"개 선택 가능)")]),p.value?(r(),n("div",ye,[...s[8]||(s[8]=[e("div",{class:"spinner"},null,-1),e("p",null,"테마 목록을 불러오는 중...",-1)])])):k.value?(r(),n("div",_e,d(k.value),1)):(r(),n("div",we,[(r(!0),n(y,null,E(X.value,(l,a)=>(r(),n("div",{key:a,class:"category-section"},[e("h3",Ce,[e("span",Te,d(U.value[a]?.emoji),1),e("span",xe,d(U.value[a]?.name),1)]),e("div",Ae,[(r(!0),n(y,null,E(l,i=>(r(),n("button",{type:"button",key:i.id,class:g(["theme-chip",{selected:i.selected}]),onClick:m=>J(i)},[e("img",{src:i.imageUrl,alt:i.label,class:"chip-icon"},null,8,je),e("span",Ie,d(i.label),1),i.selected?(r(),n("div",Ee,[...s[9]||(s[9]=[e("svg",{xmlns:"http://www.w3.org/2000/svg",width:"12",height:"12",viewBox:"0 0 24 24",fill:"none",stroke:"currentColor","stroke-width":"3","stroke-linecap":"round","stroke-linejoin":"round"},[e("path",{d:"M20 6L9 17l-5-5"})],-1)])])):f("",!0)],10,Me))),128))])]))),128))]))]),e("div",Se,[e("button",{class:"complete-btn",onClick:K,disabled:v.value||p.value},d(v.value?"가입 완료 중...":"선택 완료"),9,Le),e("button",{class:"skip-btn",onClick:ee,disabled:v.value||p.value}," 건너뛰기 ",8,Ne)])],64)):f("",!0)]),M.value?(r(),n("div",{key:0,class:"modal-overlay",onClick:S(j,["self"])},[e("div",Ve,[e("button",{class:"modal-close-btn",onClick:j},[...s[11]||(s[11]=[e("svg",{xmlns:"http://www.w3.org/2000/svg",width:"20",height:"20",viewBox:"0 0 24 24",fill:"none",stroke:"currentColor","stroke-width":"2","stroke-linecap":"round","stroke-linejoin":"round"},[e("line",{x1:"18",y1:"6",x2:"6",y2:"18"}),e("line",{x1:"6",y1:"6",x2:"18",y2:"18"})],-1)])]),e("p",qe,d(R.value),1),e("button",{class:"modal-btn",onClick:j},"확인")])])):f("",!0),x.value?(r(),n("div",{key:1,class:"modal-overlay",onClick:S(A,["self"])},[e("div",Be,[e("button",{class:"modal-close-btn",onClick:A},[...s[12]||(s[12]=[e("svg",{xmlns:"http://www.w3.org/2000/svg",width:"20",height:"20",viewBox:"0 0 24 24",fill:"none",stroke:"currentColor","stroke-width":"2","stroke-linecap":"round","stroke-linejoin":"round"},[e("line",{x1:"18",y1:"6",x2:"6",y2:"18"}),e("line",{x1:"6",y1:"6",x2:"18",y2:"18"})],-1)])]),e("h2",null,d(q.value),1),e("div",Ue,[e("div",{innerHTML:B.value},null,8,Re)]),e("button",{class:"modal-btn",onClick:A},"닫기")])])):f("",!0)]))}},He=se(De,[["__scopeId","data-v-dd292153"]]);export{He as default};
