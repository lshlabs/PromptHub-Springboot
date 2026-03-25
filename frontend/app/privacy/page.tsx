import { LegalPageLayout } from '@/components/common/legal-page-layout'

export default function PrivacyPage() {
  return (
    <LegalPageLayout
      title="개인정보처리방침"
      description="PromptHub는 서비스 운영, 로그인 인증, 커뮤니티 기능 제공을 위해 필요한 최소한의 정보를 처리합니다."
      lastUpdated="2026년 2월 24일"
      sections={[
        {
          heading: '수집하는 정보',
          paragraphs: [
            '회원가입 및 로그인 과정에서 이메일, 사용자명, 프로필 정보(선택 입력), 인증 토큰 정보가 처리될 수 있습니다.',
            '서비스 보안 및 세션 관리 목적을 위해 접속 기기 정보, 브라우저 정보, IP 주소, 세션 활동 기록이 저장될 수 있습니다.',
          ],
        },
        {
          heading: '이용 목적',
          paragraphs: [
            '회원 인증, 프로필 관리, 게시글/북마크/좋아요 기능 제공 및 서비스 운영 품질 개선을 위해 정보를 사용합니다.',
            '비정상 접근 탐지, 계정 보호, 세션 관리 등 보안 목적으로 일부 로그와 세션 정보를 처리합니다.',
          ],
        },
        {
          heading: '보관 및 삭제',
          paragraphs: [
            '계정 삭제 요청 시 관련 개인정보는 서비스 운영상 필요한 범위를 제외하고 삭제 절차를 진행합니다.',
            '법령상 보관 의무가 있는 데이터는 해당 기간 동안 별도로 보관될 수 있습니다.',
          ],
        },
      ]}
    />
  )
}
