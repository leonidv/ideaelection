import { Idea } from "../../../../../types/Idea";

export interface GroupMainScreenIdeaDialogProps {
    idea: any
    assignee: string
    author: string
    convertDate: string
    setNewIdea: (any) => void
    handleClickAssignee: () => void
    handleOptionGroup: (e: any, idea: any) => void
    handleClickVote: () => void
    votes: any
    openId: string
    showAlert: (open: any, severity: any, message: any) => void,
    isAdmin: boolean
    allIdeas: any
    fetchMoreData: (isMore?: boolean, newIdeas?: any, localURLParams?: string) => void
  }