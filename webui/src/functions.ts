import { useEffect, useState } from 'react'
import isValidDomain from 'is-valid-domain'

export const convertDate = (dateString: string) => {
  const date = new Date(dateString)
  const yyyy = date.getFullYear().toString()
  const mm = (date.getMonth() + 1).toString()
  const dd = date.getDate().toString()

  const mmChars = mm.split('')
  const ddChars = dd.split('')

  return (
    (ddChars[1] ? dd : '0' + ddChars[0]) +
    '.' +
    (mmChars[1] ? mm : '0' + mmChars[0]) +
    '.' +
    yyyy
  )
}

export const getAllUrlParams: any = url => {
  var queryString = url ? url.split('?')[1] : window.location.search.slice(1)

  var obj = {}
  if (queryString) {
    queryString = queryString.split('#')[0]
    var arr = queryString.split('&')

    for (var i = 0; i < arr.length; i++) {
      var a = arr[i].split('=')
      var paramName = a[0]
      var paramValue = typeof a[1] === 'undefined' ? true : a[1]
      paramName = paramName.toLowerCase()
      if (paramName.match(/\[(\d+)?\]$/)) {
        var key = paramName.replace(/\[(\d+)?\]/, '')
        if (!obj[key]) obj[key] = []
        if (paramName.match(/\[\d+\]$/)) {
          var index = /\[(\d+)\]/.exec(paramName)[1]
          obj[key][index] = paramValue
        } else {
          obj[key].push(paramValue)
        }
      } else {
        if (!obj[paramName]) {
          obj[paramName] = paramValue
        } else if (obj[paramName] && typeof obj[paramName] === 'string') {
          obj[paramName] = [obj[paramName]]
          obj[paramName].push(paramValue)
        } else {
          obj[paramName].push(paramValue)
        }
      }
    }
  }

  return obj
}

export const findAuthor = (idea, authorsNames) => {
  const author =
    authorsNames &&
    authorsNames.filter(author => {
      return author.id == idea.author
    })[0]

  return author && author.displayName ? author.displayName : ''
}

export const findAuthorForIdea = (idea, allIdeas, me, param) => {
  if (allIdeas && allIdeas.users) {
    const names = allIdeas.users
    const author =
      names &&
      names.filter(author => {
        return param == 'idea' ? author.id == idea.author : author.id == idea.assignee
      })[0]

    if (author && author.displayName) {
      return author.displayName
    } else {
      if (param == 'idea') {
        return me.displayName
      } else {
        return ''
      }
    }
  }
}

export const findAssignee = (idea, authorsNames) => {
  const author =
    authorsNames &&
    authorsNames.filter(author => {
      return author.id == idea.assignee
    })[0]

  return author && author.displayName ? author.displayName : ''
}

export const findAuthorById = (authorId, authorsNames) => {
  const author =
    authorsNames &&
    authorsNames.length &&
    authorsNames.filter(author => {
      return author.id == authorId
    })[0]

  return author && author.displayName ? author.displayName : ''
}

export const useWindowSize = () => {
  const [windowSize, setWindowSize] = useState({
    width: undefined,
    height: undefined
  })
  useEffect(() => {
    function handleResize () {
      setWindowSize({
        width: window.innerWidth,
        height: window.innerHeight
      })
    }
    window.addEventListener('resize', handleResize)
    handleResize()
    return () => window.removeEventListener('resize', handleResize)
  }, []) 
  return windowSize
}

export const switchParamsOrderingGroups = [
  {
    name: 'Newest first'
  },
  {
    name: 'Older first'
  },
  {
    name: 'Title A-Z'
  },
  {
    name: 'Title Z-A'
  }
]

export const switchParamsOrdering = [
  {
    name: 'Newest first'
  },
  {
    name: 'Older first'
  }
]

export const getOrdering = name => {
  let ordering = ''

  switch (name) {
    case 'Newest first':
      ordering = 'ordering=CTIME_DESC&'
      break
    case 'Older first':
      ordering = 'ordering=CTIME_ASC&'
      break
    case 'Title A-Z':
      ordering = 'ordering=NAME_ASC&'
      break
    case 'Title Z-A':
      ordering = 'ordering=NAME_DESC&'
      break
  }

  return ordering
}

export const modes = ['PUBLIC', 'CLOSED', 'PRIVATE']

export const groupParamsDefault = {
  name: '',
  description: '',
  logo: '',
  entryMode: '',
  entryQuestion: '',
  domainRestrictions: []
}

export const isValidateGroupParams = (params, showAlert) => {
  if (params.domainRestrictions.length) {
    params.domainRestrictions.split(', ').forEach(restr => {
      if (!isValidDomain(restr, { subdomain: true })) {
        showAlert(true, 'error', 'The domain is not valid')
        return false
      }
    })
  }

  if (!params.logo) {
    showAlert(true, 'error', 'You need to upload a picture')
    return false
  }

  if (params.logo.length > 150000) {
    showAlert(true, 'error', 'The picture is too big')
    return false
  }

  if (params.name.length < 3) {
    showAlert(true, 'error', 'The group must have a name')
    return false
  }

  if (params.description.length < 3) {
    showAlert(true, 'error', 'The group must have a description')
    return false
  }

  if (!params.entryMode) {
    showAlert(true, 'error', 'The group must have an entry mode')
    return false
  }

  return true
}

export const isJson = str => {
  const JSON_START = /^\[|^\{(?!\{)/
  const JSON_ENDS = {
    '[': /]$/,
    '{': /}$/
  }
  var jsonStart = str.match(JSON_START)
  return jsonStart && JSON_ENDS[jsonStart[0]].test(str)
}

export const createListOptions = (isAdmin, me, idea) => {
  const newOptions = []
  if (idea.archived) {
    if (isAdmin || idea.assignee == me.sub || idea.author == me.sub) {
      newOptions.push('Unarchived')
    }
  } else if (idea.implemented) {
    if (isAdmin || idea.assignee == me.sub) {
      newOptions.push('Mark as undone')
    }
  } else {
    if (isAdmin || me.sub == idea.assignee) {
      newOptions.push('Remove assignee', 'Mark as Done')
    }

    if (isAdmin) {
      newOptions.push('Change assignee')
    }

    if (isAdmin || me.sub == idea.assignee || me.sub == idea.author) {
      newOptions.push('Archive')
    }

    if (
      isAdmin
      // isAdmin ||
      // idea.assignee == me.sub ||
      // (!idea.assignee && me.sub == idea.author)
    ) {
      newOptions.push('Move to group...')
    }
  }

  if (isAdmin || (!idea.voters.length && me.sub == idea.author)) {
    newOptions.push('Delete')
  }

  return newOptions
}

export const validURL = str => {
  var pattern = new RegExp(
    '^(https?:\\/\\/)?' +
      '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|' +
      '((\\d{1,3}\\.){3}\\d{1,3}))' +
      '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*' +
      '(\\?[;&a-z\\d%_.~+=-]*)?' +
      '(\\#[-a-z\\d_]*)?$',
    'i'
  )
  return !!pattern.test(str)
}

export const whiteImg =
  'data:image/png;base64,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'
