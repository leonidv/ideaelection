export interface GroupMainScreenCardCommentsListContentProps {
  ideaId: string
  token: string
  comment: {
    author: string
    content: string
    ctime: string
    id: string
    ideaId: string
    lastEditedBy: null | string
    lastEditedTime: null | string
    replyTo: null | string
  }
  author: {
    avatar: string
    displayName: string
    domain: string
    email: string
    externalId: string
    id: string
    ctime: string
    roles: any
    subscriptionPlan: string
  }
  t: (string) => string
  showAlert: (open: any, severity: any, message: any) => void
  setComments: any
  comments: any
  fetchMoreData?: (a, b) => void
  isOptions: boolean
}
