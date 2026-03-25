'use client'

import { ExtensionHeader } from '@/components/extension/extension-header'
import ExtensionTabs, { ExtensionCTA } from '@/components/extension/extension-tabs'
import { useState } from 'react'

export default function ExtensionPage() {
  const [activeTab, setActiveTab] = useState('features')

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="mx-auto max-w-7xl space-y-8">
          {/* 헤더 섹션 */}
          <div className="mb-8">
            <ExtensionHeader />
          </div>

          {/* 탭 섹션 */}
          <ExtensionTabs activeTab={activeTab} onTabChange={setActiveTab} />

          {/* CTA 섹션 */}
          <ExtensionCTA onDemoClick={() => setActiveTab('demo')} />
        </div>
      </div>
    </div>
  )
}
