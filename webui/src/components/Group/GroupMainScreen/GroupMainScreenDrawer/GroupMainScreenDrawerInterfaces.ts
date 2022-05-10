import { Groups } from '../../../../types/Groups'
import { Me } from '../../../../types/Me'
import { Members } from '../../../../types/Members'

export interface GroupMainScreenDrawerProps {
  param?: string
  switchParams?: [
    {
      title: string
      name: string
    }
  ]
  state?: any
  handleChangeSwitch?: (Event) => void
  handleManageJoinRequests?: () => void
  members?: Members[]
  setMembers?: any
  handleInviteMembers?: () => void
  handleClickSettings?: () => void
  group?: Groups
  openGroupMainScreenSettings?: boolean
  setOpenGroupMainScreenSettings?: (boolean) => void
  handleClose?: () => void
  me?: Me
}
