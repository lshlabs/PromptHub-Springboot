import { redirect } from 'next/navigation';

/**
 * 루트 경로 리다이렉트 페이지
 *
 * Next.js App Router에서 루트 경로(/)로 접근 시
 * 홈페이지(/home)로 자동 리다이렉트됩니다.
 *
 * 이렇게 구성한 이유:
 * - 다른 페이지들과 일관된 폴더 구조 유지
 * - 향후 다국어 지원 시 /en/home, /ko/home 등으로 확장 가능
 * - 페이지별 레이아웃 관리 용이성
 */
export default function RootPage() {
  // 홈페이지로 리다이렉트
  redirect('/home');
}
