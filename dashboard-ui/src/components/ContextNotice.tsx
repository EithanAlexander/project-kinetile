import type { ReactNode } from 'react'

export interface ContextNoticeProps {
  readonly title: string
  readonly children: ReactNode
}

/** Amber contextual notice (hardware fallback, filtered charts, etc.). */
export default function ContextNotice({ title, children }: ContextNoticeProps) {
  return (
    <output className="rgf-notice">
      <p className="rgf-notice-title">{title}</p>
      <p className="rgf-notice-body">{children}</p>
    </output>
  )
}
