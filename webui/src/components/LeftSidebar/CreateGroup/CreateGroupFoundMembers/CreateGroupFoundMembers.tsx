import { useEffect, useRef } from 'react'
import { MenuItem, Avatar } from '@material-ui/core'

import { CreateGroupFoundMembersProps } from './CreateGroupFoundMembersInterfaces'

import './CreateGroupFoundMembers.scss'
import { fetchMembers } from '../../../../functionsRequests'
import { useRecoilValue } from 'recoil'
import { meInfoState, tokenState } from '../../../../state'

export const CreateGroupFoundMembers = (
  props: CreateGroupFoundMembersProps
) => {
  const {
    setAnchorElFoundMembers,
    anchorElFoundMembers,
    foundMembers,
    membersListCurrent,
    setMembersListCurrent,
    setCurrentMembers,
    maxMembers
  } = props

  let ref = useRef(null)

  const token = useRecoilValue(tokenState)
  const me = useRecoilValue(meInfoState)

  useEffect(() => {
    setCurrentMembers(membersListCurrent.length)
  }, [...membersListCurrent])

  const handleClickFoundMember = member => {
    const tmpArray = []

    function itemCheck (item) {
      if (
        tmpArray.indexOf(item.id) === -1 &&
        tmpArray.indexOf(item.userId) === -1
      ) {
        tmpArray.push(item.userId || item.id)
        return true
      }
      return false
    }

    if (
      (+maxMembers && membersListCurrent.length < maxMembers) ||
      maxMembers == '\u221E'
    ) {
      setMembersListCurrent(
        membersListCurrent.concat(member).filter(member => itemCheck(member))
      )
      setCurrentMembers(membersListCurrent.length)
    }

    setAnchorElFoundMembers(null)
  }

  const handleClickFoundGroup = group => {
    const tmpArray = []

    function itemCheck (item) {
      if (item.userId == me.sub) {
        return false
      }

      if (
        tmpArray.indexOf(item.id) === -1 &&
        tmpArray.indexOf(item.userId) === -1
      ) {
        tmpArray.push(item.userId || item.id)
        return true
      }
      return false
    }

    if (
      (+maxMembers && membersListCurrent.length < maxMembers) ||
      maxMembers == '\u221E'
    ) {
      fetchMembers(token, group.id).then(members => {
        setMembersListCurrent(
          membersListCurrent.concat(members).filter(member => itemCheck(member))
        )
        setCurrentMembers(membersListCurrent.length)
      })
    }

    setAnchorElFoundMembers(null)
  }

  useEffect(() => {
    document.addEventListener('click', handleClickOutside, true)
    return () => {
      document.removeEventListener('click', handleClickOutside, true)
    }
  })

  const handleClickOutside = event => {
    if (ref.current && !ref.current.contains(event.target)) {
      handleClose()
    }
  }

  const handleClose = () => {
    setAnchorElFoundMembers(null)
  }

  if (
    (foundMembers && foundMembers.members && foundMembers.members.length > 0) ||
    (foundMembers && foundMembers.groups && foundMembers.groups.length > 0)
  ) {
    return (
      <div ref={ref} className='createGroupFoundMembers'>
        <div
          id='foundMembers'
          className={`createGroupFoundMembers__menu ${
            Boolean(anchorElFoundMembers)
              ? 'createGroupFoundMembers__menu--active'
              : ''
          }`}
        >
          {foundMembers &&
            foundMembers.members.map(member => (
              <MenuItem
                className='creatGroupFoundMembers__menuItem row'
                key={member.id}
                onClick={() => handleClickFoundMember(member)}
              >
                <Avatar
                  className='createGroupMember__avatar'
                  alt={member.displayName}
                  src={member.avatar}
                />
                {member.displayName}
              </MenuItem>
            ))}

          {foundMembers &&
            foundMembers.groups.map(group => (
              <MenuItem
                className='creatGroupFoundMembers__menuItem row'
                key={group.id}
                onClick={() => handleClickFoundGroup(group)}
              >
                <Avatar
                  className='createGroupMember__avatar'
                  alt={group.name}
                  src={group.logo}
                />
                {group.name}
              </MenuItem>
            ))}
        </div>
      </div>
    )
  } else {
    return <></>
  }
}
