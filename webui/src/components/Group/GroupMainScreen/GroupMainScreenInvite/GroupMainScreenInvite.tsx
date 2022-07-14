import { useEffect, useState } from 'react'
import { useRecoilState, useRecoilValue, useSetRecoilState } from 'recoil'
import { useTranslation } from 'react-i18next'
import {
  Card,
  CardHeader,
  Avatar,
  Button,
  Popper,
  ClickAwayListener,
  TextField
} from '@material-ui/core'

import {
  allUserGroupsState,
  membersState,
  tokenState
} from './../../../../state'
import { convertDate, findAuthorById, whiteImg } from '../../../../functions'
import {
  resoluteInvite,
  postJoinRequest,
  revokeRequest,
  resoluteJoinRequest,
  fetchMembers
} from '../../../../functionsRequests'
import { Modal } from '../../../Modal/Modal'
import { ModalButtons } from '../../../Modal/ModalButtons/ModalButtons'

import { GroupMainScreenInviteProps } from './GroupMainScreenInviteInterfaces'

import './GroupMainScreenInvite.scss'
import { Alert } from '@material-ui/lab'

export const GroupMainScreenInvite: React.FC<GroupMainScreenInviteProps> = (
  props: GroupMainScreenInviteProps
) => {
  const {
    invite,
    invites,
    setInvites,
    param,
    isParamFound,
    localParam,
    isModal,
    fetchMoreDataInvites,
    fetchMoreData,
    handleCloseJoinRequestModal,
    showAlert
  } = props

  const token = useRecoilValue(tokenState)
  const [avatar, setAvatar] = useState('')
  const [user, setUser]: any = useState([])
  const [isAdmin, setIsAdmin] = useState(false)
  const [isMyRequest, setIsMyRequest] = useState(false)
  const [message, setMessage] = useState('')
  const [curMessage, setCurMessage] = useState('')
  const [curTime, setCurTime] = useState('')
  const [openModalRequest, setOpenModalRequest] = useState(false)
  const [status, setStatus] = useState(invite.status || '')
  const [groups, setGroups]: any = useRecoilState(allUserGroupsState)
  const [isInGroup, setIsInGroup] = useState(false)
  const setMembers = useSetRecoilState(membersState)

  const authorId = invite.userId
  const [author, setAuthor] = useState(
    invite.creator
      ? invite.creator.displayName
      : findAuthorById(authorId, invite.users)
  )

  const { t } = useTranslation()

  useEffect(() => {
    if (localParam && invites && invites.users) {
      setUser(invites.users.filter(user => user.id == invite.userId)[0])
      if (user) {
        setAvatar(user.avatar)
      }
    }
  }, [localParam, user])

  useEffect(() => {
    if (authorId) {
      setAuthor(findAuthorById(authorId, invite.users))
    }
  }, [authorId])

  useEffect(() => {}, [isMyRequest])

  useEffect(() => {
    setIsAdmin(false)

    if (invites && invites.groups && invites.groups.length) {
      invites.groups.map(group => {
        invite.author == group.creator.id ? setIsAdmin(true) : ''
      })
    }
    if (invites && invites.joinRequests && invites.joinRequests.length) {
      invites.joinRequests.map(req => {
        req.groupId == invite.id ? setIsMyRequest(true) : ''
      })
    }

    if (param == 'available' && isModal) {
      groups.map(group => {
        group.id == invites[0].id ? setIsInGroup(true) : ''
      })
    }

    if (!param && !localParam) {
      const curInvite = invites.invites.filter(
        inv => inv.groupId == invite.id
      )[0]
      setCurMessage(curInvite.message)
      setCurTime(convertDate(curInvite.mtime))
    }

    if (param == 'requests') {
      const curInvite = invites.joinRequests.filter(
        inv => inv.groupId == invite.id
      )[0]
      setCurMessage(curInvite.message)
      setCurTime(convertDate(curInvite.mtime))
    }

    if (localParam) {
      setCurTime(convertDate(invite.mtime))
    }
  }, [invite])

  const handleCloseModalRequest = () => {
    setOpenModalRequest(false)
  }

  const handleCLickInvite = (localInviteGroup, localStatus) => {
    let localInviteId = ''
    if (!localParam) {
      const invitesList = invites.invites
      if (invitesList && invitesList.length > 1) {
        invitesList.forEach(inviteItem => {
          if (inviteItem.groupId == localInviteGroup.id) {
            localInviteId = inviteItem.id
          }
        })
      } else {
        if (invitesList[0].groupId == localInviteGroup.id) {
          localInviteId = invitesList[0].id
        }
      }

      ;(async () => {
        const result = await resoluteInvite(localInviteId, token, localStatus)
        setStatus(await result.status)
        if (invites && invites.groups && invites.groups.length) {
          let newInvites = Object.assign({}, invites)
          newInvites.groups = await invites.groups.filter(
            group => group.id !== localInviteGroup.id
          )

          if (localStatus !== 'DECLINED') {
            fetchMoreData(
              true,
              [
                invites.groups.filter(group => {
                  return group.id == invite.id
                })[0],
                ...groups
              ],
              'add'
            )
          }

          fetchMoreDataInvites(true, newInvites)
        }
      })()
    } else {
      ;(async () => {
        const result = await resoluteJoinRequest(invite.id, token, localStatus)
        setStatus(await result.status)
        let newInvites = Object.assign({}, invites)
        newInvites.joinRequests = await invites.joinRequests.filter(
          joinRequest => joinRequest.id !== invite.id
        )

        showAlert(true, 'success', t('Join request is accepted succesfully'))

        const newMembers = await fetchMembers(token, invite.groupId)

        if ((await newMembers) !== 'undefined') {
          setMembers(await newMembers)
        }

        setInvites(newInvites)
      })()
    }
  }

  const handleChangeMessage = e => {
    setMessage(e.target.value)
  }

  const createRequestToGroup = () => {
    const result: any = postJoinRequest(token, invite.joiningKey, message)
    result.then(res => {
      if (res) {
        const status = res.joinRequest.status
        if (status == 'APPROVED' || status == 'UNRESOLVED') {
          if (status == 'APPROVED') {
            let newInvites = Object.assign({}, invites)
            if (newInvites.groups) {
              newInvites.groups = invites.groups.filter(curInvite => {
                return curInvite.id !== invite.id
              })
            }

            if (invites && invites.groups && invites.groups.length) {
              fetchMoreData(
                true,
                [
                  invites.groups.filter(group => {
                    return group.id == invite.id
                  })[0],
                  ...groups
                ],
                'add'
              )

              fetchMoreDataInvites(true, newInvites)
            }

            if (isModal) {
              showAlert(true, 'success', t('Invite is accepting succesfully'))
              handleCloseJoinRequestModal()
            } else {
              showAlert(true, 'success', t('Invite is accepting succesfully'))
            }

            if (isParamFound) {
              setTimeout(() => {
                window.location.reload()
              }, 1000)
            }
          } else {
            let newInvites = Object.assign({}, invites)
            if (newInvites.groups) {
              newInvites.groups = invites.groups.map(group => {
                return group.id == invite.id ? { ...group, status } : group
              })
            }
            setIsMyRequest(true)

            if (isModal) {
              handleCloseJoinRequestModal()
              showAlert(
                true,
                'success',
                t('Join request is sending succesfully')
              )
            } else {
              showAlert(
                true,
                'success',
                t('Join request is sending succesfully')
              )
            }
          }
        }
      } else {
        if (isModal) {
          showAlert(true, 'error', 'Something went wrong')
          handleCloseJoinRequestModal()
        }
      }
    })

    setOpenModalRequest(false)
  }

  const handleClickRevoke = () => {
    const requestId = invites.joinRequests.filter(joinRequest => {
      return joinRequest.groupId == invite.id
    })[0].id
    ;(async () => {
      const result = await revokeRequest(token, requestId)
      if ((await result) && (await result).status == 'REVOKED') {
        showAlert(true, 'success', 'Join request successfully revoked')

        let newInvites = Object.assign({}, invites)
        newInvites.groups = await invites.groups.filter(curInvite => {
          return curInvite.id !== invite.id
        })
        fetchMoreDataInvites(true, newInvites)
      } else {
        showAlert(true, 'error', 'Something went wrong')
      }
    })()
  }

  const handleCLickInviteRequests = e => {
    const option = e.target.innerText

    switch (option) {
      case t('ACCEPT INVITE'):
        createRequestToGroup()
        break
      case t('JOIN TO GROUP'):
        createRequestToGroup()
        break
      case t('REQUEST ACCESS'):
        setOpenModalRequest(true)
        break
    }
  }

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(anchorEl ? null : event.currentTarget)
  }

  const open = Boolean(anchorEl)
  const id = open ? 'simple-popper' : undefined

  const handleClose = () => {
    setAnchorEl(null)
  }

  if (invite.id) {
    return (
      status !== 'DECLINED' && (
        <div className='groupMainScreenInvite'>
          <Card className='groupMainScreenIdea__card groupMainScreenInvite__card'>
            {!localParam && (
              <CardHeader
                className='groupMainScreenInvite__cardHeader'
                title={invite.name}
                subheader={
                  param == 'available'
                    ? `${invite.membersCount} ${t('members')}, ${
                        invite.ideasCount
                      } ${t('ideas')}  ${convertDate(invite.ctime)}`
                    : param == 'requests'
                    ? `${t('You sent request')} ${curTime}`
                    : `${author} ${t('invited you')} ${curTime}`
                }
                avatar={
                  <img
                    className='groupMainScreenInvite__avatar avatar'
                    src={invite.logo}
                    onError={({ currentTarget }) => {
                      currentTarget.onerror = null
                      currentTarget.src = whiteImg
                    }}
                  />
                }
              />
            )}

            {localParam && (
              <div className='groupMainScreenInvite__cardHeader cardHeader row'>
                <div className='row'>
                  <Avatar
                    className='cardHeader__avatar'
                    alt='avatar'
                    src={avatar}
                  />

                  <div className='cardHeader__col'>
                    <p className='cardHeader__name'>{user.displayName}</p>
                    <p className='cardHeader__email'>{user.email}</p>
                  </div>
                </div>
                <p className='cardHeader__time'>{curTime}</p>
              </div>
            )}

            <div className='groupMainScreenInvite__description'>
              {param == 'available'
                ? invite.description
                : localParam || !param || param == 'requests'
                ? invite.message || curMessage
                : invite.entryQuestion}
            </div>
            <div className='groupMainScreenInvite__actions'>
              {param !== 'available' && !localParam && (
                <>
                  <Button
                    aria-describedby={id}
                    onClick={handleClick}
                    className='groupMainScreenInvite__info'
                  >
                    {t('GROUP INFO')}
                  </Button>
                  <Popper
                    className='groupMainScreenInvite__popper'
                    id={id}
                    open={open}
                    anchorEl={anchorEl}
                    disablePortal={true}
                  >
                    <ClickAwayListener onClickAway={handleClose}>
                      <div className='groupMainScreenInvite__tooltip'>
                        <div className='groupMainScreenInvite__row'>
                          <p className='groupMainScreenInvite__counts'>
                            {invite.membersCount} {t('members')},{' '}
                            {invite.ideasCount} {t('ideas')}
                          </p>
                          <p className='tooltip__date'>
                            {convertDate(invite.ctime)}
                          </p>
                        </div>
                        <p className='groupMainScreenInvite__text'>
                          {invite.description}
                        </p>
                      </div>
                    </ClickAwayListener>
                  </Popper>
                </>
              )}
              <div className='groupMainScreenInvite__btnGroup'>
                {param !== 'available' && param !== 'requests' && (
                  <>
                    <Button
                      className='groupMainScreenInvite__deny'
                      onClick={() => handleCLickInvite(invite, 'DECLINED')}
                    >
                      {t('DENY')}
                    </Button>
                    <Button
                      className='groupMainScreenInvite__accept'
                      variant='contained'
                      color='primary'
                      onClick={() => handleCLickInvite(invite, 'APPROVED')}
                    >
                      {t('ACCEPT')}
                    </Button>
                  </>
                )}

                {param == 'requests' && (
                  <Button
                    className='groupMainScreenInvite__revoke'
                    variant='contained'
                    color='primary'
                    onClick={handleClickRevoke}
                  >
                    {t('REVOKE')}
                  </Button>
                )}

                {param == 'available' && !isMyRequest && !isInGroup && (
                  <Button
                    className='groupMainScreenInvite__accept'
                    variant='contained'
                    color='primary'
                    onClick={handleCLickInviteRequests}
                  >
                    {isAdmin && t('ACCEPT INVITE')}
                    {!isAdmin &&
                      invite.entryMode == 'PUBLIC' &&
                      t('JOIN TO GROUP')}
                    {!isAdmin &&
                      (invite.entryMode == 'CLOSED' ||
                        invite.entryMode == 'PRIVATE') &&
                      t('REQUEST ACCESS')}
                  </Button>
                )}
              </div>
            </div>
            {param == 'available' &&
              isMyRequest &&
              invite.entryMode == 'CLOSED' && (
                <p className='groupMainScreenInvite__resolved'>
                  {t('YOU REQUEST IS STILL NOT RESOLVED')}
                </p>
              )}
            {param == 'available' && isInGroup && (
              <p className='groupMainScreenInvite__resolved'>
                {t('YOU HAVE ALREADY JOINED')}
              </p>
            )}
          </Card>
          <Modal
            open={openModalRequest}
            onClose={handleCloseModalRequest}
            title={t('Group`s request message')}
          >
            <p className='modalRequest__description'>
              {invite.entryQuestion && t('Administrator of the group wants')}
              {!invite.entryQuestion &&
                t(
                  "If you want you can write a message for group's administrator"
                )}
            </p>
            <p className='modalRequest__question'>
              {invite.entryQuestion && invite.entryQuestion}
            </p>
            <TextField
              name='message'
              label={invite.entryQuestion ? t('Answer') : t('Message')}
              multiline
              rows={12}
              fullWidth
              required={invite.entryQuestion ? true : false}
              className='modalRequest__message'
              defaultValue=''
              variant='outlined'
              InputLabelProps={{
                shrink: true
              }}
              onChange={handleChangeMessage}
            />
            <ModalButtons
              acceptText={t('REQUEST')}
              handleClose={handleCloseModalRequest}
              handleAccept={createRequestToGroup}
            />
          </Modal>
        </div>
      )
    )
  } else {
    return (
      <>
        <Alert severity='error' className='groupMainScreenInvite__alert'>
          <p>{t('Looks like link not found')} :(</p>
          <Button variant='contained' color='primary'>
            <a href='/main' className='mainScreen__link'>
              {t('Go to main page')}
            </a>
          </Button>
        </Alert>
      </>
    )
  }
}
