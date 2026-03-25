import { LegalPageLayout } from '@/components/common/legal-page-layout'

export default function CookiesPage() {
  return (
    <LegalPageLayout
      title="쿠키 정책"
      description="PromptHub는 로그인 상태 유지와 사용자 경험 개선을 위해 쿠키 및 유사 저장소(localStorage 등)를 사용할 수 있습니다."
      lastUpdated="2026년 2월 24일"
      sections={[
        {
          heading: '사용 목적',
          paragraphs: [
            '인증 상태 유지, 로그인 세션 관리, 사용자 선호 설정(예: 테마/언어) 저장을 위해 쿠키 또는 브라우저 저장소를 사용할 수 있습니다.',
            '서비스 성능 개선 및 오류 분석을 위해 익명화된 기술적 정보가 활용될 수 있습니다.',
          ],
        },
        {
          heading: '쿠키 제어',
          paragraphs: [
            '사용자는 브라우저 설정에서 쿠키 저장을 허용/차단하거나 기존 쿠키를 삭제할 수 있습니다.',
            '다만 일부 쿠키를 차단하면 로그인 유지, 사용자 설정 저장 등 일부 기능이 정상 동작하지 않을 수 있습니다.',
          ],
        },
        {
          heading: '정책 변경',
          paragraphs: [
            '서비스 변경 또는 법령/정책 요구사항에 따라 본 쿠키 정책은 수정될 수 있습니다.',
            '중요 변경 시 서비스 내 공지 또는 관련 페이지를 통해 업데이트 내용을 안내합니다.',
          ],
        },
      ]}
    />
  )
}
