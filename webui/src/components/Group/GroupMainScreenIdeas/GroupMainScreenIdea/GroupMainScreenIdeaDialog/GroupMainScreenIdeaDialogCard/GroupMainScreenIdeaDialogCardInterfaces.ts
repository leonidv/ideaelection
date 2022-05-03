import { Idea } from "../../../../../../types/Idea";

export interface GroupMainScreenIdeaDialogCardProps {
    idea: Idea
    assignee: string
    convertDate: string
    author: string
    handleEdit: () => void
    edit: any
    setEdit: any
    setNewIdea: (any) => void
    setOpenDialog: (boolean) => void
    handleClickAssignee: () => void
    handleOptionGroup: (e: any, idea: any) => void
    votes: any
    handleClickVote: () => void
    openId: string
    showAlert: (open: any, severity: any, message: any) => void
    isAdmin: boolean
    allIdeas: any
    isEditAccess: boolean
    fetchMoreData: (isMore?: boolean, newIdeas?: any, localURLParams?: string) => void
}