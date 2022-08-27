const BACKEND_API_URL = process.env.BACKEND_API_URL

const fetchRequest: any = async (
  token,
  url,
  method = 'GET',
  body,
  content = null
) => {
  try {
    const response = await fetch(url, {
      method: method,
      credentials: 'include',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': content || 'text/plain'
      },
      body: body
    })

    const responseData = await response.json()

    if (responseData.data) {
      return responseData.data
    }
  } catch (error) {
    console.log(error)
  }
}

export const fetchAuthor = async (authorId: string, token: string) => {
  const url = `${BACKEND_API_URL}/users/${authorId}`
  const request = await fetchRequest(token, url, 'GET', null)
  if ((await request) && authorId) {
    return await request.displayName
  } else {
    return 'undefined'
  }
}

export const fetchMe = async (token: string, userId: string) => {
  const url = `${BACKEND_API_URL}/users/${userId}`
  const request = await fetchRequest(token, url, 'GET', null)
  if ((await request) && userId) {
    return await request
  } else {
    return 'undefined'
  }
}

export const fetchGroup = async (token: string, groupId: string) => {
  const url = `${BACKEND_API_URL}/groups/${groupId}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  } else {
    return []
  }
}

export const fetchGroupByKey = async (token: string, key: string) => {
  const url = `${BACKEND_API_URL}/groups?key=${key}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  } else {
    return []
  }
}

export const updateToken = async token => {
  const url = `${BACKEND_API_URL}/token`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  } else {
    return ''
  }
}

export const fetchRequests = async (
  token,
  userId,
  urlParams = null,
  skip = 0,
  count = 10
) => {
  const url = `${BACKEND_API_URL}/joinrequests?userId=${userId}&${
    urlParams ? urlParams : ''
  }skip=${skip}&count=${count}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  } else {
    return 'undefined'
  }
}

export const fetchAllGroups = async (token, userId, skip = 0, count = 10) => {
  const url = `${BACKEND_API_URL}/groups?userId=${userId}&skip=${skip}&count=${count}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  } else {
    return 'undefined'
  }
}

export const fetchGroups = async (token, userId, name = '') => {
  const url = name
    ? `${BACKEND_API_URL}/groups?userId=${userId}&name=${name}`
    : `${BACKEND_API_URL}/groups?userId=${userId}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request.groups
  } else {
    return 'undefined'
  }
}

export const searchMembersByName = async (letters, token) => {
  const url = `${BACKEND_API_URL}/users?filter=${letters}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  }
}

export const searchMembersByNameFromGroup = async (
  token,
  userId,
  groupName
) => {
  const url = `${BACKEND_API_URL}/groups?userId=${userId}&name=${groupName}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  }
}

export const deleteIdea = async (token, ideaId) => {
  const url = `${BACKEND_API_URL}/ideas/${ideaId}`
  const request = await fetchRequest(token, url, 'DELETE', null)
  if (await request) {
    return await request.idea
  } else {
    return 'undefined'
  }
}

export const removeMember = async (token, groupId, memberId) => {
  const url = `${BACKEND_API_URL}/groups/${groupId}/members/${memberId}`
  const request = await fetchRequest(token, url, 'DELETE', null)
  if (await request) {
    return await request
  } else {
    return 'undefined'
  }
}

export const resoluteInvite = async (inviteId, token, status) => {
  const url = `${BACKEND_API_URL}/invites/${inviteId}/status`
  const body = JSON.stringify({ status: status })
  const request = await fetchRequest(
    token,
    url,
    'PATCH',
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}

export const resoluteJoinRequest = async (joinRequestId, token, status) => {
  const url = `${BACKEND_API_URL}/joinrequests/${joinRequestId}/status`
  const body = JSON.stringify({ status: status })
  const request = await fetchRequest(
    token,
    url,
    'PATCH',
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}

export const patchAssignee = async (token, ideaId, body) => {
  const url = `${BACKEND_API_URL}/ideas/${ideaId}/assignee`
  const request = await fetchRequest(
    token,
    url,
    'PATCH',
    body,
    'application/json'
  )
  if (await request) {
    return await request.idea
  } else {
    return 'undefined'
  }
}

export const patchMoveToGroup = async (token, ideaId, body) => {
  const url = `${BACKEND_API_URL}/ideas/${ideaId}/group`
  const request = await fetchRequest(
    token,
    url,
    'PATCH',
    body,
    'application/json'
  )
  if (await request) {
    return await request.idea
  } else {
    return 'undefined'
  }
}

export const patchIdeaArchive = async (token, ideaId, archived) => {
  const body = JSON.stringify({
    archived: archived
  })
  const url = `${BACKEND_API_URL}/ideas/${ideaId}/archived`
  const request = await fetchRequest(
    token,
    url,
    'PATCH',
    body,
    'application/json'
  )
  if (await request) {
    return await request.idea
  } else {
    return 'undefined'
  }
}

export const patchIdeaImplemented = async (token, ideaId, implemented) => {
  const body = JSON.stringify({
    implemented: implemented
  })
  const url = `${BACKEND_API_URL}/ideas/${ideaId}/implemented`
  const request = await fetchRequest(
    token,
    url,
    'PATCH',
    body,
    'application/json'
  )
  if (await request) {
    return await request.idea
  } else {
    return 'undefined'
  }
}

export const postVote = async (token, ideaId, method) => {
  const url = `${BACKEND_API_URL}/ideas/${ideaId}/voters`
  const request = await fetchRequest(
    token,
    url,
    method,
    null,
    'application/json'
  )
  if (await request) {
    return await request.idea
  } else {
    return 'undefined'
  }
}

export const postInvites = async (token, groupId, members, message) => {
  let users = []
  if (members.length) {
    members.forEach(member => users.push(member.id))
  }

  const url = `${BACKEND_API_URL}/invites/`
  const body = JSON.stringify({
    groupId: groupId,
    message: message,
    registeredUsersIds: users,
    newUsersEmails: []
  })
  const request = await fetchRequest(
    token,
    url,
    'POST',
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}

export const putSettings = async (
  token,
  settings,
  planSettings,
  tariff?: string
) => {
  const url = `${BACKEND_API_URL}/users/settings`
  const body = JSON.stringify({
    displayName: planSettings.name,
    subscriptionPlan: tariff || planSettings.subscriptionPlan,
    settings: {
      notificationsFrequency: settings.notifications.toUpperCase(),
      subscribedToNews: settings.checked
    }
  })
  const request = await fetchRequest(
    token,
    url,
    'PUT',
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}

export const postNewIdea = async (
  token: string,
  method: string,
  url: string,
  ideaParams
) => {
  const body = JSON.stringify(ideaParams)
  const request = await fetchRequest(
    token,
    url,
    method,
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}

export const postNewGroup = async (token: string, groupParams) => {
  const url = `${BACKEND_API_URL}/groups`
  const params = Object.assign({}, groupParams)
  if (params.domainRestrictions.length) {
    params.domainRestrictions = groupParams.domainRestrictions.split(',')
  } else {
    params.domainRestrictions = []
  }

  const body = JSON.stringify(params)
  const request = await fetchRequest(
    token,
    url,
    'POST',
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}

export const patchGroup = async (token: string, groupParams, groupId) => {
  const url = `${BACKEND_API_URL}/groups/${groupId}`
  if (groupParams.domainRestrictions.length) {
    groupParams.domainRestrictions = groupParams.domainRestrictions.split(',')
  } else {
    groupParams.domainRestrictions = []
  }

  const body = JSON.stringify(groupParams)
  const request = await fetchRequest(
    token,
    url,
    'PATCH',
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}

export const postJoinRequest = async (token, joiningKey, message) => {
  const url = `${BACKEND_API_URL}/joinrequests/`
  const body = JSON.stringify({
    joiningKey: joiningKey,
    message: message
  })
  const request = await fetchRequest(
    token,
    url,
    'POST',
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}

export const fetchMembers = async (token, groupId) => {
  const url = `${BACKEND_API_URL}/groups/${groupId}/members`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  }
}

export const fetchGroupRequests = async (token, groupId) => {
  const url = `${BACKEND_API_URL}/joinrequests?groupId=${groupId}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  }
}

export const fetchIdeas = async (
  token,
  groupId,
  urlParams = null,
  skip = 0,
  count = 10
) => {
  const url = `${BACKEND_API_URL}/ideas?groupId=${groupId}&skip=${skip}&count=${count}&${urlParams}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return request
  } else {
    return 'undefined'
  }
}

export const fetchIdeaById = async (token, ideaId) => {
  const url = `${BACKEND_API_URL}/ideas/${ideaId}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return request
  } else {
    return 'undefined'
  }
}

export const fetchInvites = async (
  token,
  userId,
  urlParams = null,
  skip = 0,
  count = 10
) => {
  const url = `${BACKEND_API_URL}/invites?userId=${userId}&${
    urlParams ? urlParams : ''
  }skip=${skip}&count=${count}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return request
  } else {
    return 'undefined'
  }
}

export const fetchAvailibaleGroups = async (
  token: string,
  urlParams = null,
  skip = 0,
  count = 10
) => {
  const url = `${BACKEND_API_URL}/groups?onlyAvailable=true&${
    urlParams ? urlParams : ''
  }skip=${skip}&count=${count}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  }
}

export const fetchMembersSearch = async (token, groupId, search) => {
  const url = `${BACKEND_API_URL}/groups/${groupId}/members?username=${search}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  }
}

export const revokeRequest = async (token, requestId) => {
  const url = `${BACKEND_API_URL}/joinrequests/${requestId}`
  const request = await fetchRequest(token, url, 'DELETE', null)
  if (await request) {
    return await request
  }
}

export const updateJoiningKey = async (token, groupId) => {
  const url = `${BACKEND_API_URL}/groups/${groupId}/joining-key`
  const request = await fetchRequest(token, url, 'DELETE', null)
  if (await request) {
    return await request
  }
}

export const leaveGroup = async (
  token: string,
  groupId: string,
  userId: string
) => {
  const url = `${BACKEND_API_URL}/groups/${groupId}/members/${userId}`
  const request = await fetchRequest(token, url, 'DELETE', null)
  if (await request) {
    return await request
  }
}

export const deleteGroup = async (token: string, groupId: string) => {
  const url = `${BACKEND_API_URL}/groups/${groupId}`
  const request = await fetchRequest(token, url, 'DELETE', null)
  if (await request) {
    return await request
  }
}

export const fetchAccoutSettings = async (token: string) => {
  const url = `${BACKEND_API_URL}/users/settings`

  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return await request
  }
}

export const tryGetToken = async token => {
  const url = `${BACKEND_API_URL}/token`
  const request = await fetchRequest(token, url, 'GET', null)

  if (await request) {
    return await request
  }
}

export const postNewComment = async (
  token: string,
  ideaId: string,
  commentContent: any
) => {
  const url = `${BACKEND_API_URL}/ideas/${ideaId}/comments`

  const body = JSON.stringify(commentContent)
  const request = await fetchRequest(
    token,
    url,
    'POST',
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}

export const fetchComments = async (
  token: string,
  ideaId: string,
  skip = 0,
  count = 10
) => {
  const url = `${BACKEND_API_URL}/ideas/${ideaId}/comments?skip=${skip}&count=${count}`
  const request = await fetchRequest(token, url, 'GET', null)
  if (await request) {
    return request
  } else {
    return 'undefined'
  }
}

export const deleteComment = async (token, ideaId, commentId) => {
  const url = `${BACKEND_API_URL}/ideas/${ideaId}/comments/${commentId}`
  const request = await fetchRequest(token, url, 'DELETE', null)
  if (await request) {
    return await request
  } else {
    return 'undefined'
  }
}

export const editComment = async (
  token: string,
  ideaId: string,
  commentId: string,
  commentContent: any
) => {
  const url = `${BACKEND_API_URL}/ideas/${ideaId}/comments/${commentId}`

  const body = JSON.stringify(commentContent)
  const request = await fetchRequest(
    token,
    url,
    'PUT',
    body,
    'application/json'
  )
  if (await request) {
    return await request
  }
}
