/**
 * AccountManagement 컴포넌트
 *
 * 설정 페이지의 계정 관리 섹션을 표시합니다.
 * 계정 삭제 기능을 포함합니다.
 */

'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'

interface AccountManagementProps {
  onDeleteAccount: (confirmation: string) => void
}

export function AccountManagement({ onDeleteAccount }: AccountManagementProps) {
  const [deleteAccountConfirmation, setDeleteAccountConfirmation] = useState('')

  const handleDeleteAccount = () => {
    onDeleteAccount(deleteAccountConfirmation)
  }

  return (
    <div className="grid gap-4">
      <div className="rounded-lg border border-red-200 bg-red-50 p-4">
        <h3 className="mb-2 font-medium text-red-800">계정 삭제</h3>
        <p className="mb-4 text-sm text-red-600">
          계정을 삭제하면 모든 데이터가 영구적으로 삭제되며 복구할 수 없습니다.
        </p>
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button
              variant="destructive"
              size="sm"
              className="bg-red-600 text-white hover:bg-red-700">
              계정 삭제
            </Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>정말로 계정을 삭제하시겠습니까?</AlertDialogTitle>
              <AlertDialogDescription>
                이 작업은 되돌릴 수 없습니다. 모든 데이터가 영구적으로 삭제됩니다. 계정을 삭제하려면
                아래에 "계정 삭제"를 입력하세요.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <Input
              placeholder="계정 삭제"
              value={deleteAccountConfirmation}
              onChange={e => setDeleteAccountConfirmation(e.target.value)}
              className="mt-4"
            />
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleDeleteAccount}
                className="bg-red-600 text-white hover:bg-red-700">
                계정 삭제
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  )
}
