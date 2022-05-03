import { atom, selector } from 'recoil'
import { Me } from './types/Me'
import { Groups, defaultGroups } from './types/Groups'
import {
  fetchIdeas,
  fetchMembers,
  fetchInvites,
  fetchIdeaById,
  fetchGroupByKey,
  fetchGroup
} from './functionsRequests'
import { AvailablesGroups } from './types/AvailablesGroups'
import { Members } from './types/Members'

import jwt_decode from "jwt-decode";

export const token = selector({
  key: 'token',
  get: ({ get }) => get(tokenState),
  set: ({ set, get }, newValue) => {
    set(tokenState, newValue)
    get(meInfoState)
  }
})

export const tokenState = atom({
  key: 'tokenState',
  default: localStorage.getItem('jwt') || ''
})

export const maxMembersState = atom({
  key: 'maxMembersState',
  default: 10
})

export const groupNameState = atom({
  key: 'groupNameState',
  default: ''
})

export const currentGroupIdFromLocationState = atom({
  key: 'currentGroupIdFromLocationState',
  default:
    window.location.href.split('/')[3] == 'group'
      ? window.location.href.split('/')[4]
      : ''
})

export const meInfoState = selector<Me>({
  key: 'meInfoState',
  get: async ({ get }) => {
    try {
      const token = get(tokenState) || localStorage.getItem('jwt')

      if (typeof token == 'string') {
        const decodedToken:Me = jwt_decode(token)
        return decodedToken
      }
      return null
  } catch (error) {
    throw new Error(`Error in 'allUserGroups': ${error.message}`)
  }
},
set ({ set }, newValue) {
  set(infoMeAtomState, newValue)
}
})

export const infoMeAtomState = atom<Me>({
  key: 'infoMeAtomState',
  default: meInfoState
})

export const allUserGroups = selector<Groups[]>({
  key: 'allUserGroups',
  get: async ({ get }) => {
    try {
      if (get(infoMeAtomState)) {
        const userId = get(infoMeAtomState).sub
        if (userId) {
          const token = get(tokenState)

          const response = await fetch(
            `https://api.test.saedi.io/groups?userId=${userId} `,
            {
              credentials: 'include',
              headers: {
                'Access-Control-Allow-Origin': 'true',
                Authorization: `Bearer ${token}`
              }
            }
          )

          const responseData = await response.json()

          return responseData.data.groups || []
        }
      }
      return []
    } catch (error) {
      throw new Error(`Error in 'allUserGroups': ${error.message}`)
      return []
    }
  },
  set ({ set }, newValue) {
    set(allUserGroupsState, newValue)
  }
})

export const allUserGroupsState = atom<Groups[]>({
  key: 'allUserGroupsState',
  default: allUserGroups
})

export const invitesSelector = selector({
  key: 'invitesSelector',
  get: async ({ get }) => {
    try {
      if (get(infoMeAtomState)) {
        const userId = get(infoMeAtomState).sub
        if (userId) {
          const token = get(tokenState)
          return await fetchInvites(token, userId, null)
        }
        return []
      }
      return false
    } catch (error) {
      throw new Error(`Error in 'invitesSelector': ${error.message}`)
    }
  },
  set ({ set }, newValue) {
    set(invitesState, newValue)
  }
})

export const invitesState = atom({
  key: 'invitesState',
  default: invitesSelector
})

export const groupIdState = atom({
  key: 'groupIdState',
  default: localStorage.getItem('currentGroupId') || ''
})

export const joinRequestsSelector = selector({
  key: 'joinRequestsSelector',
  get: async ({ get }) => {
    try {
      if (get(infoMeAtomState)) {
        const userId = get(infoMeAtomState).sub
        if (userId) {
         return { groups: [], joinRequests: [], invites: [] }
        }
        return { groups: [], joinRequests: [], invites: [] }
      }
      return false
    } catch (error) {
      throw new Error(`Error in 'joinRequestsSelector': ${error.message}`)
    }
  },
  set ({ set }, newValue) {
    set(joinRequestsState, newValue)
  }
})

export const joinRequestsState = atom({
  key: 'joinRequestsState',
  default: joinRequestsSelector
})

export const currentGroupSelector = selector<Groups>({
  key: 'currentGroupSelector',
  get: ({ get }) => {
    const token = get(tokenState)
    try {
      if (token && window.location.href.split('/')[3] == 'group') {
        return fetchGroup(token, window.location.href.split('/')[4]).then(
          res => {
            if (res !== 'undefined' && res.id) {
             
              return res
            }
          }
        )
      }
      return defaultGroups
    } catch (error) {
      throw new Error(`Error in 'currentGroupSelector': ${error.message}`)
    }
    // return get(currentGroupState)
  },
  set ({ set, get }, newValue) {
    set(currentGroupState, newValue)
  }
})

export const currentGroupState = atom<Groups>({
  key: 'currentGroupState',
  default: currentGroupSelector || defaultGroups
})

export const ideasSelector = selector({
  key: 'ideasSelector',
  get: async ({ get }) => {
    try {
      let groupId = ''

      if (window.location.href.split('/')[3] == 'ideas') {
        const ideaId = window.location.href.split('/')[4]
        if (ideaId) {
          const token = get(tokenState)

          const idea = await fetchIdeaById(token, ideaId)

          if ((await idea) !== 'undefined') {
            groupId = idea.idea.groupId
            localStorage.setItem('currentGroupId', groupId)
            if (groupId) {
              return await fetchIdeas(token, groupId)
            }
          }
        }
      } else if (
        get(currentGroupState) ||
        get(currentGroupIdFromLocationState)
      ) {
        groupId =
          get(currentGroupState).id || get(currentGroupIdFromLocationState)
        if (groupId) {
          const token = get(tokenState)

          return await fetchIdeas(token, groupId)
        }
      }
    } catch (error) {
      throw new Error(`Error in 'ideasSelector': ${error.message}`)
    }
    return { ideas: [], users: [] }
  },
  set: ({ set }, newValue) => {
    return set(ideasState, newValue)
  }
})

export const ideasState = atom({
  key: 'ideasState',
  default: ideasSelector
})

export const availablesGroupsSelector = selector<AvailablesGroups>({
  key: 'availablesGroupsSelector',
  get: async ({ get }) => {
    const token = get(tokenState)
    if (token) {
      try {
        return { groups: [], joinRequests: [], invites: [] }
      } catch (error) {
        throw new Error(`Error in 'AvailablesGroupsState': ${error.message}`)
      }
    }
    return { groups: [], joinRequests: [], invites: [] }
  },
  set ({ set }, newValue) {
    set(availablesGroupsState, newValue)
  }
})

export const availablesGroupsState = atom<AvailablesGroups>({
  key: 'availablesGroupsState',
  default: availablesGroupsSelector
})

export const membersSelector = selector<Members[]>({
  key: 'membersSelector',
  get: async ({ get }) => {
    get(currentGroupState)
    get(currentGroupIdFromLocationState)
  
    const token = get(tokenState)
    if (token && window.location.href.split('/')[3] == 'group') {
      const groupId = window.location.href.split('/')[4]

      if (groupId) {
        try {
          return await fetchMembers(token, groupId)
        } catch (error) {
          throw new Error(`Error in 'membersState': ${error.message}`)
        }
      }
    }
    return []
  },
  set ({ get }, newValue) {
    get(membersState)
  }
})

export const membersState = atom<Members[]>({
  key: 'membersState',
  default: membersSelector
})

export const groupByKeySelector = selector({
  key: 'groupByKeySelector',
  get: async ({ get }) => {
    const token = get(tokenState)

    if (token) {
      const key = window.location.href.split('inviteLink=')[1]
      localStorage.setItem('inviteKey', key)
      try {
        return fetchGroupByKey(token, key).then(res => {
          if (res) {
            return Array(1).fill(res)
          }
        })
      } catch (error) {
        throw new Error(`Error in 'groupByKeyState': ${error.message}`)
      }
    }

    return []
  },
  set ({ set }, newValue) {
    set(groupByKeyState, newValue)
  }
})

export const groupByKeyState = atom({
  key: 'groupByKeyState',
  default: groupByKeySelector
})

export const authorsNamesSelector = selector({
  key: 'authorsNamesSelector',
  get: ({ get }) => {
    if (get(authorsNamesState)) return get(authorsNamesState)
  },
  set: ({ set }, newValue) => set(authorsNamesState, newValue)
})

export const authorsNamesState = atom({
  key: 'authorsNamesState',
  default: []
})

export const mobileOpenSelector = selector({
  key: 'mobileOpenSelector',
  get: ({ get }) => {
    return get(mobileOpenState)
  },
  set: ({ set, get }) => set(mobileOpenState, !get(mobileOpenState))
})

export const mobileOpenState = atom({
  key: 'mobileOpenState',
  default: false
})

export const createGroupOpenSelector = selector({
  key: 'createGroupOpenSelector',
  get: ({ get }) => {
    return get(createGroupOpenState)
  },
  set: ({ set, get }) => set(createGroupOpenState, !get(createGroupOpenState))
})

export const createGroupOpenState = atom({
  key: 'createGroupOpenState',
  default: false
})

export const mobileOpenRightSelector = selector({
  key: 'mobileOpenRightSelector',
  get: ({ get }) => {
    return get(mobileOpenRightState)
  },
  set: ({ set, get }) => set(mobileOpenRightState, !get(mobileOpenRightState))
})

export const mobileOpenRightState = atom({
  key: 'mobileOpenRightState',
  default: false
})

export const groupFiltersSelector = selector({
  key: 'groupFiltersSelector',
  get: ({ get }) => {
    return get(groupFilterState)
  },
  set ({ set }, newValue) {
    set(groupFilterState, newValue)
    localStorage.setItem('groupFilterState', JSON.stringify(newValue))
  }
})

export const groupFilterState = atom({
  key: 'groupFilterState',
  default: JSON.parse(localStorage.getItem('groupFilterState')) || {
    id: 'states'
  }
})
