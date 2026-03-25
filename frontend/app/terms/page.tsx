import { LegalPageLayout } from '@/components/common/legal-page-layout'

export default function TermsPage() {
  return (
    <LegalPageLayout
      title="이용약관"
      description="PromptHub 이용 시 아래 약관이 적용됩니다. 서비스 이용을 계속하는 경우 본 약관에 동의한 것으로 간주됩니다."
      lastUpdated="2026년 2월 24일"
      sections={[
        {
          heading: '서비스 이용',
          paragraphs: [
            '사용자는 관련 법령 및 본 약관을 준수하여 서비스를 이용해야 하며, 타인의 권리를 침해하는 콘텐츠를 게시해서는 안 됩니다.',
            '서비스는 기능 개선, 안정화, 정책 변경 등을 위해 일부 기능이 변경되거나 중단될 수 있습니다.',
          ],
        },
        {
          heading: '사용자 콘텐츠',
          paragraphs: [
            '사용자가 작성한 게시글, 댓글, 프로필 정보에 대한 책임은 작성자에게 있습니다.',
            '운영 정책 위반 콘텐츠는 사전 통지 없이 비공개 처리 또는 삭제될 수 있습니다.',
          ],
        },
        {
          heading: '면책 및 책임 제한',
          paragraphs: [
            'PromptHub는 사용자 생성 콘텐츠의 정확성, 완전성, 유용성을 보증하지 않습니다.',
            '서비스 장애, 외부 플랫폼 연동 이슈, 사용자 과실로 인해 발생한 손해에 대해 법령이 허용하는 범위에서 책임이 제한될 수 있습니다.',
          ],
        },
      ]}
    />
  )
}
