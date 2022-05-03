import { Idea } from '../../../../types/Idea'
import { Members } from '../../../../types/Members';

export interface GroupMainScreenIdeaProps {
  author: string
  assignee: string
  openId: string
  ideas: Idea[]
  setIdeas: (any) => void
  idea: Idea
  findAssignee: (idea: Idea) => string
  members: Members[]
  fetchMoreData: (isMore?: boolean, newIdeas?: any, localURLParams?: string) => void

}
