import { Groups } from './Groups'
import { JoinRequest } from './JoinRequest'
import { Invite } from './Invite'

export type AvailablesGroups =
  | {
      groups: Groups[]
      joinRequests: JoinRequest[]
      invites: Invite[]
      notifications?: null | any
    }
  | { groups: []; joinRequests: []; invites: [] }
