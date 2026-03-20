import type { PropsWithChildren, ReactNode } from 'react'

interface AsyncStateProps extends PropsWithChildren {
  loading?: boolean
  error?: string
  empty?: boolean
  emptyDescription?: string
  loadingFallback?: ReactNode
}

export default function AsyncState({
  loading,
  error,
  empty,
  emptyDescription = '暂无数据',
  loadingFallback,
  children,
}: AsyncStateProps) {
  if (loading) {
    return (
      <div className="flex min-h-48 items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-slate-50 text-sm text-slate-500">
        {loadingFallback || '加载中...'}
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex min-h-48 items-center justify-center rounded-3xl border border-rose-200 bg-rose-50 px-6 text-sm text-rose-600">
        {error}
      </div>
    )
  }

  if (empty) {
    return (
      <div className="flex min-h-48 items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-slate-50 px-6 text-sm text-slate-500">
        {emptyDescription}
      </div>
    )
  }

  return <>{children}</>
}
