import { ReactNode } from 'react'
import { Groups } from '../../types/Groups';

export interface LeftSideBarProp {
  mobileOpen: boolean
  handleDrawerToggle: () => void
  children?: ReactNode
  showAlert: (a,b,c) => void
  fetchMoreData: (a?:boolean, b?) => void,
  isFetching: boolean
  localGroups: Groups[]
  setLocalGroups: any
}
