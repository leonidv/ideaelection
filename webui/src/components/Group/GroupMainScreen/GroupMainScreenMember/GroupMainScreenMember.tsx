import { useEffect, useRef } from 'react'
import { useRecoilValue } from 'recoil'

import { Avatar } from '@material-ui/core'
import { GroupMainScreenOptions } from '../GroupMainScreenOptions/GroupMainScreenOptions'
import { currentGroupState, tokenState } from '../../../../state'
import { removeMember } from '../../../../functionsRequests'

import { GroupMainScreenMemberProps } from './GroupMainScreenMemberInterfaces'

import { useTranslation } from 'react-i18next'

import './GroupMainScreenMember.scss'

export const GroupMainScreenMember = (props: GroupMainScreenMemberProps) => {
  const {
    member,
    param,
    changedMember,
    setChangedMember,
    paramMain,
    showAlert,
    isAdmin,
    members,
    setMembers,
    setLocalMembers
  } = props

  const token = useRecoilValue(tokenState)
  const groupId = useRecoilValue(currentGroupState).id
  const { t } = useTranslation()

  const anchorRef = useRef(null)

  const options = [t('Remove')]

  useEffect(() => {}, [changedMember])

  const handleSelectMember = selectMember => {
    setChangedMember(param == 'group' ? selectMember.id : selectMember.userId)
  }

  const handleOption = e => {
    const option = e.target.innerText

    switch (option) {
      case t('Remove'):
        removeMember(token, groupId, member.userId).then(res => {
          if (res == 'OK') {
            const newMembersList = members.filter(
              cur => cur.userId !== member.userId
            )

            setMembers(newMembersList)
            setLocalMembers(newMembersList)
            showAlert(true, 'success', t('Member deleted succesfully'))
          } else {
            showAlert(true, 'error', t('Member cannot be removed'))
          }
        })
        break
    }
  }

  return (
    <div
      onClick={() => {
        !paramMain && handleSelectMember(member)
      }}
      className={`${
        (param == 'group' ? member.id : member.userId) == changedMember
          ? 'groupMainScreenMember--active'
          : ''
      } 
                ${
                  param == 'select' || param == 'group'
                    ? 'groupMainScreenMember--pointer'
                    : ''
                }
                groupMainScreenMember row`}
    >
      <Avatar
        className='groupMainScreenMember__avatar'
        alt='avatar'
        src={member.avatar ? member.avatar : member.logo}
      />
      <div className='groupMainScreenMember__col col'>
        <p className='groupMainScreenMember__name'>
          {param == 'group' ? member.name : member.displayName}
        </p>
        {member.roleInGroup == 'GROUP_ADMIN' && paramMain && (
          <p className='groupMainScreenMember__role'>{t('group admin')}</p>
        )}
      </div>
      {paramMain && isAdmin && (
        <GroupMainScreenOptions
          options={options}
          id={member.id}
          handleOption={handleOption}
          anchorRef={anchorRef}
        />
      )}
    </div>
  )
}
