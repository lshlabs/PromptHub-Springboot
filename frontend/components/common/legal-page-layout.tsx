import Link from 'next/link'

interface LegalPageLayoutProps {
  title: string
  description: string
  lastUpdated: string
  sections: Array<{ heading: string; paragraphs: string[] }>
}

export function LegalPageLayout({
  title,
  description,
  lastUpdated,
  sections,
}: LegalPageLayoutProps) {
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-10">
        <div className="mx-auto max-w-4xl space-y-6">
          <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm sm:p-8">
            <div className="mb-4 flex items-center justify-between gap-3">
              <h1 className="text-2xl font-bold text-gray-900 sm:text-3xl">{title}</h1>
              <Link
                href="/"
                className="rounded-md border border-gray-200 px-3 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-50">
                홈으로
              </Link>
            </div>
            <p className="text-sm leading-6 text-gray-600 sm:text-base">{description}</p>
            <p className="mt-3 text-xs text-gray-500">최종 업데이트: {lastUpdated}</p>
          </div>

          <div className="space-y-4">
            {sections.map(section => (
              <section
                key={section.heading}
                className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm sm:p-8">
                <h2 className="mb-3 text-lg font-semibold text-gray-900">{section.heading}</h2>
                <div className="space-y-3 text-sm leading-6 text-gray-700 sm:text-base">
                  {section.paragraphs.map((paragraph, index) => (
                    <p key={`${section.heading}-${index}`}>{paragraph}</p>
                  ))}
                </div>
              </section>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
