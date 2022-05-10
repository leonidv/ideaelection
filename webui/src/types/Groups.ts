export type Groups = {
  id: string
  ctime: string
  creator: {
    id: string
    email: string
    displayName: string
    avatar: string
  }
  state: string
  name: string
  description: string
  logo: string
  entryMode: string
  entryQuestion: string
  domainRestrictions: string[]
  membersCount: number
  ideasCount: number
  joiningKey: string
  deleted: boolean
}

export const defaultGroups = {
  id: '',
  ctime: '',
  creator: {
    id: '',
    email: '',
    displayName: '',
    avatar: ''
  },
  state: '',
  name: '',
  description: '',
  logo: '',
  entryMode: '',
  entryQuestion: '',
  domainRestrictions: [''],
  membersCount: 0,
  ideasCount: 0,
  joiningKey: '',
  deleted: false
}
