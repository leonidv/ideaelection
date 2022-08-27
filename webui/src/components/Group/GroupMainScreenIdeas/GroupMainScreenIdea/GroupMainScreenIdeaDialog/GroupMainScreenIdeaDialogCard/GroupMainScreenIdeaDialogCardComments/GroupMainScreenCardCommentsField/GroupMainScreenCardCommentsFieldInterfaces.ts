export interface GroupMainScreenCardCommentsFieldProps {
  ideaId: string
  token: string
  t: (string) => string
  showAlert: (open: any, severity: any, message: any) => void
  isReply?: boolean
  setIsReply?: (a: boolean) => void
  isEdit?: boolean
  setIsEdit?: (a: boolean) => void
  fetchMoreData: (a: boolean, b: any) => void
  comments: any
  commentContent?: any
  setCommentContent?
  setComments?
  comment?
}

export const defaultCommentContent = {
  content: null,
  replyTo: null
}
