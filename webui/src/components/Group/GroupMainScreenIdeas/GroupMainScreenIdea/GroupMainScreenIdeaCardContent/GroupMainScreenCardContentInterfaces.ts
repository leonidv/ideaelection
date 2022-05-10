import { Idea } from '../../../../../types/Idea'

export interface GroupMainScreenCardContentProps {
  idea: Idea
  handleClickAssignee: () => void
  handleOptionGroup: (e: any, idea: Idea) => void
  handleClickVote: () => void
  handleOpenModal?: () => void
  assignee: string
  votes: number
  options: any[]
  isExtendedMode?: boolean
  allIdeas: any
}
