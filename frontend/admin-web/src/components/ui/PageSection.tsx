import type { PropsWithChildren, ReactNode } from 'react'

interface PageSectionProps extends PropsWithChildren {
  title: string
  description?: string
  action?: ReactNode
}

export default function PageSection({ title, description, action, children }: PageSectionProps) {
  return (
    <section className="panel p-6 sm:p-7">
      <div className="mb-5 flex flex-col gap-3 border-b border-slate-100 pb-5 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
          {description ? <p className="mt-1 text-sm leading-6 text-slate-500">{description}</p> : null}
        </div>
        {action ? <div className="flex shrink-0 flex-wrap gap-2">{action}</div> : null}
      </div>
      {children}
    </section>
  )
}
