import { useState, useEffect, Dispatch, SetStateAction } from 'react'
import { useRecoilValue } from 'recoil'
import { useTranslation } from 'react-i18next'

import { Button, Divider, TextField } from '@material-ui/core'
import { Tooltip } from '../../../Tooltip/Tooltip'
import { CreateGroupMember } from '../CreateGroupMember/CreateGroupMember'
import { CreateGroupFoundMembers } from '../CreateGroupFoundMembers/CreateGroupFoundMembers'
import { Modal } from '../../../Modal/Modal'
import { ModalButtons } from '../../../Modal/ModalButtons/ModalButtons'

import { meInfoState, membersState, tokenState } from '../../../../state'
import { CreateGroupInviteMembersProps } from './CreateGroupInviteMembersInterfaces'
import { Members } from '../../../../types/Members'
import { Me } from '../../../../types/Me'

import {
  searchMembersByName,
  searchMembersByNameFromGroup
} from '../../../../functionsRequests'

import './CreateGroupInviteMembers.scss'
import { groupParamsDefault } from '../../../../functions'

export const CreateGroupInviteMembers = (
  props: CreateGroupInviteMembersProps
) => {
  const {
    handleCreateGroup,
    members,
    setMembers,
    inviteMessage,
    setInviteMessage,
    setNext,
    isNew,
    setGroupParams
  } = props

  const [currentMembersCount, setCurrentMembersCount]: [
    number,
    Dispatch<SetStateAction<number>>
  ] = useState(isNew ? 0 : members.length)
  const currentMembers: Members[] = isNew ? [] : useRecoilValue(membersState)
  const me: Me = useRecoilValue(meInfoState)
  const token: string = useRecoilValue(tokenState)
  const [maxMembers, setMaxMembers]: [
    string,
    Dispatch<SetStateAction<string>>
  ] = useState('\u221E')
  const [foundMembers, setFoundMembers] = useState({ groups: [], members: [] })

  const [anchorElFoundMembers, setAnchorElFoundMembers]: [
    any,
    Dispatch<SetStateAction<any>>
  ] = useState(null)
  const [searchMembers, setSearchMembers]: [
    string,
    Dispatch<SetStateAction<string>>
  ] = useState('')
  const [isLimit, setIsLimit]: [
    boolean,
    Dispatch<SetStateAction<Boolean>>
  ] = useState(false)
  const [isOpen, setIsOpen]: [
    boolean,
    Dispatch<SetStateAction<Boolean>>
  ] = useState(true)

  const { t } = useTranslation()

  useEffect(() => {}, [currentMembersCount, foundMembers, currentMembers])

  useEffect(() => {
    setCurrentMembersCount(members.length)
  }, [members])

  const handleClose = () => {
    setInviteMessage('')
    setMembers([])
    setCurrentMembersCount(0)
    setIsOpen(false)
    setNext(false)
    if (typeof setGroupParams == 'function') {
      setGroupParams(groupParamsDefault)
    }
  }

  const handleDeleteMember = (id: string) => {
    const delMembmers: Members[] | [] = members.filter(
      member => member.id !== id
    )
    setMembers(delMembmers)
    setCurrentMembersCount(delMembmers.length)
  }

  const handleChangeSearchMembers = e => {
    const { value } = e.target

    if (value.length > 2) {
      if (value[0] == '@') {
        searchMembersByNameFromGroup(token, me.sub, value.slice(1)).then(
          foundedGroups => {
            if (foundedGroups && foundedGroups.groups.length) {
              setFoundMembers({ members: [], groups: foundedGroups.groups })
              setAnchorElFoundMembers(e.target)
            } else {
              setFoundMembers({ groups: [], members: [] })
            }
          }
        )
      } else {
        const currentFoundMembers: any = searchMembersByName(value, token)
        ;(async () => {
          const membersList = await currentFoundMembers
          if (membersList.length > 0) {
            setFoundMembers({
              groups: [],
              members: await membersList.filter(member => member.id !== me.sub)
            })
            setAnchorElFoundMembers(e.target)
          } else {
            setFoundMembers({ groups: [], members: [] })
          }
        })()
      }
    } else {
      setFoundMembers({ groups: [], members: [] })
    }
    setSearchMembers(value)
  }

  const handleChangeInvite = e => {
    const { value } = e.target
    setInviteMessage(value)
  }

  return (
    <div className='createGroup'>
      <Modal
        open={isOpen}
        onClose={handleClose}
        className='createGroupInviteMembers__modal'
      >
        <div className='createGroup__top row'>
          <h1 className='createGroupInviteMembers__title'>
            {t('Invite members')}
          </h1>

          <div className='createGroup__top-right'>
            <span className='createGroup__members'>
              {currentMembersCount}/
              <span className={isLimit ? 'createGroup--limit' : ''}>
                {maxMembers}
              </span>
            </span>
            <Tooltip title={t('The number of users added to the group')} />
          </div>
        </div>
        <div className='createGroupInviteMembers__content'>
          <TextField
            name='invite'
            className='createGroup__field'
            label={t('Invite message')}
            margin='dense'
            fullWidth
            value={inviteMessage}
            InputLabelProps={{
              shrink: true
            }}
            variant='outlined'
            onChange={handleChangeInvite}
          />
          <div className='row createGroup__row createGroupInviteMembers__invite-row'>
            <TextField
              name='searchMembers'
              label={t('User or group')}
              margin='dense'
              fullWidth
              value={searchMembers}
              InputLabelProps={{
                shrink: true
              }}
              variant='outlined'
              onChange={handleChangeSearchMembers}
            />
            <Button className='createGroup__btn-field'>{t('ADD')}</Button>
            {Boolean(anchorElFoundMembers) && (
              <CreateGroupFoundMembers
                setAnchorElFoundMembers={setAnchorElFoundMembers}
                anchorElFoundMembers={anchorElFoundMembers}
                foundMembers={foundMembers}
                membersListCurrent={members}
                setMembersListCurrent={setMembers}
                setCurrentMembers={setCurrentMembersCount}
                maxMembers={maxMembers}
              />
            )}
          </div>

          <Divider />
          <div className='createGroup__listMembers'>
            {members &&
              members.map(member => (
                <CreateGroupMember
                  key={member.id}
                  member={member}
                  handleDeleteMember={handleDeleteMember}
                />
              ))}
          </div>
        </div>
        <ModalButtons
          handleClose={handleClose}
          handleAccept={handleCreateGroup}
          acceptText={t('Create')}
        />
      </Modal>
    </div>
  )
}
