import { Groups } from '../../../types/Groups'

export interface createGroupProps {
  copyGroup?: Groups
  setCopyGroup?: (Groups) => void
  fetchMoreData?: (a:boolean, b:Groups[], c:boolean) => void
}
