import { MutableRefObject, ReactNode } from 'react'

export interface modalProps {
  open: boolean
  onClose: (event) => void
  title?: string
  className?: string
  children: ReactNode
  ref?: MutableRefObject<any> | null
}
