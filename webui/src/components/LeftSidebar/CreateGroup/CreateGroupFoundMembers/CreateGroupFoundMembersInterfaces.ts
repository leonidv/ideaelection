import { Members } from '../../../../types/Members'

export interface CreateGroupFoundMembersProps {
  setAnchorElFoundMembers: (any) => void
  anchorElFoundMembers: any
  foundMembers: {groups: any, members: any}
  membersListCurrent: Members[]
  setMembersListCurrent: (any) => void
  setCurrentMembers: (number) => void
  maxMembers: number | string
}
