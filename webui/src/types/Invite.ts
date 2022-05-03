export type Invite = {
  groupId: string
  userId: string
  author: string
  userEmail: string | null
  emailWasSent: string | null
  status: string
  message: string
  ctime: string
  mtime: string
  id: string
  isForPerson: boolean
  isForUser: boolean
}
