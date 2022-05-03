import { Members } from '../../../../types/Members'

export interface CreateGroupInviteMembersProps {
  handleCreateGroup: () => void
  setNext: (boolean) => void
  members: Members[]
  setMembers: (any) => void
  inviteMessage: string
  setInviteMessage: (string) => void
  isNew?: boolean
  membersPlan?: number | string
  groupParams?: { name: string; description: string; logo: string; entryMode: string; entryQuestion: string; domainRestrictions: any[]; },
  setGroupParams?: (any) => void
}
