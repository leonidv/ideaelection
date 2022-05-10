import { Groups } from '../../../types/Groups'
import { Members } from '../../../types/Members'

export interface SearchShortListProps {
  changedItem?: Groups | Members
  setChangedItem?: (any) => void
  param: string
  isAdmin?: boolean
  curMembers?: Members[]
  setMembers?: any
}
