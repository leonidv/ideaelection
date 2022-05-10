import { ReactNode } from 'react'
import { Groups } from '../../types/Groups';

export interface LocationState {
  group: any
  state: {
    group: {
      id: string
    }
  }
}

export interface GroupProp {
  openId?: string
   allGroups?: Groups[]
  // isFetching: boolean
  // setIsFetching: any
}
