import { Avatar, IconButton } from '@material-ui/core/'
import RemoveIcon from '@material-ui/icons/Remove'
import { useEffect } from 'react'

import { CreateGroupMemberProps } from './CreateGroupInterfaces'

import './CreateGroupMember.scss'

export const CreateGroupMember = (
  props: CreateGroupMemberProps
): JSX.Element => {
  const { member, handleDeleteMember } = props

  useEffect(() => {}, [member])

  return (
    <div className='createGroupMember row'>
      <Avatar
        className='createGroupMember__avatar'
        alt={member.displayName}
        src={member.avatar}
      />
      <div className='col createGroupMember__col'>
        <h2 className='createGroupMember__name'>{member.displayName}</h2>
        <p className='createGroupMember__mail'>{member.email}</p>
      </div>

      <IconButton
        className='createGroupMember__btn'
        onClick={() => handleDeleteMember(member.id)}
      >
        <RemoveIcon />
      </IconButton>
    </div>
  )
}
