import { useState, useEffect, Fragment } from 'react'
import { useRecoilValue, useRecoilState, useSetRecoilState } from 'recoil'
import { useTranslation } from 'react-i18next'

import { Button, MenuItem, TextField } from '@material-ui/core/'
import AddPhotoAlternateOutlinedIcon from '@material-ui/icons/AddPhotoAlternateOutlined'
import FileInputComponent from 'react-file-input-previews-base64'

import {
  tokenState,
  allUserGroupsState,
  createGroupOpenState,
  currentGroupState,
  meInfoState
} from './../../../state'
import {
  fetchMembers,
  postInvites,
  postNewGroup
} from '../../../functionsRequests'

import { CreateGroupInviteMembers } from './CreateGroupInviteMembers/CreateGroupInviteMembers'
import { ShowAlert } from '../../Alert/Alert'
import { ModalButtons } from '../../Modal/ModalButtons/ModalButtons'
import { Modal } from '../../Modal/Modal'
import { Tooltip } from '../../Tooltip/Tooltip'

import { createGroupProps } from './CreateGroupInterfaces'
import {
  isValidateGroupParams,
  modes,
  groupParamsDefault
} from '../../../functions'

import './createGroup.scss'
import { useHistory } from 'react-router'

declare global {
  interface JSON {
    parse(text: Buffer, reviver?: (key: any, value: any) => any): any
  }
}

export const CreateGroup: React.FC<createGroupProps> = (
  props: createGroupProps
) => {
  const { copyGroup, setCopyGroup, fetchMoreData } = props
  const [groupParams, setGroupParams] = useState(groupParamsDefault)

  const token: any = useRecoilValue(tokenState) || ''
  const [groups, setGroups] = useRecoilState(allUserGroupsState)

  const [open, setOpen] = useState(false)
  const [isCopy, setIsCopy] = useState(false)
  const [inviteMessage, setInviteMessage] = useState('')
  const [members, setMembers] = useState([])
  const [next, setNext] = useState(false)
  const [backgroundLoad, setBackgroundLoad]: any = useState({})

  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')
  const [openSnackbar, setOpenSnackbar] = useState(false)

  const setCreateGroupOpen = useSetRecoilState(createGroupOpenState)
  const setCurrentGroup = useSetRecoilState(currentGroupState)
  const me = useRecoilValue(meInfoState)

  const history = useHistory()

  const { t } = useTranslation()

  useEffect(() => {
    if (copyGroup.id && !open) {
      setOpen(true)
      setIsCopy(true)

      fetchMembers(token, copyGroup.id).then(res => {
        if (res !== 'undefined') {
          const newMembers = res.filter(member => member.userId !== me.sub)
          setMembers(newMembers)
        }
      })

      copyGroup.entryMode
        ? setGroupParams(prevData => {
            return {
              ...prevData,
              ['entryMode']: copyGroup.entryMode,
              ['entryQuestion']: copyGroup.entryQuestion,
              ['domainRestrictions']: copyGroup.domainRestrictions,
              ['logo']: ''
            }
          })
        : ''
      setMembers([])
    }
  }, [copyGroup])

  const showAlert = (open, severity, message) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)
    setTimeout(() => setOpenSnackbar(false), 3000)
  }

  const handleClickOpen = () => {
    setOpen(true)
    setCreateGroupOpen(true)
  }

  const handleClose = () => {
    setCreateGroupOpen(false)
    setNext(false)
    setIsCopy(false)
    setCopyGroup([])
    setGroupParams(groupParamsDefault)
    setBackgroundLoad({})
    setOpen(false)
    return
  }

  const saveFile = file => {
    setBackgroundLoad({ backgroundImage: `url("${file.base64}")` })
    document.querySelector<HTMLElement>(
      '.createGroup__fileInput button svg'
    ).style.visibility = 'hidden'
    setGroupParams(prevData => {
      return {
        ...prevData,
        ['logo']: file.base64
      }
    })
  }

  const handleChange = e => {
    let { name, value } = e.target

    if (name == 'entryMode') value = value.toUpperCase()

    setGroupParams(prevData => {
      return {
        ...prevData,
        [name]: value
      }
    })
  }

  const handleNextForm = () => {
    if (isValidateGroupParams(groupParams, showAlert)) {
      setOpen(false)
      setNext(true)
      setBackgroundLoad({ backgroundImage: '' })
    } else if (!token) {
      showAlert(true, 'error', t('You need to log in again'))
    }
  }

  const handleCreateGroup = () => {
    if (isValidateGroupParams(groupParams, showAlert)) {
      ;(async () => {
        const newGroup = await postNewGroup(token, groupParams)

        // console.log(await newGroup == 'undefined');

        if (
          (await newGroup) !== 'undefined' &&
          (await newGroup) !== undefined &&
          (await newGroup)
        ) {
          showAlert(true, 'success', t('Group successfully created'))
          setGroups([newGroup, ...groups])
          setNext(false)
          setOpen(false)

          setIsCopy(false)
          setCopyGroup([])
          setGroupParams({
            name: '',
            description: '',
            logo: '',
            entryMode: '',
            entryQuestion: '',
            domainRestrictions: []
          })

          postInvites(token, (await newGroup).id, members, inviteMessage)
          setInviteMessage('')
          setMembers([])
          setCreateGroupOpen(false)
          setCurrentGroup(newGroup.id)
          fetchMoreData(true, [newGroup, ...groups], true)
          history.push(`/group/${newGroup.id}`)
          // window.location.href=`/group/${newGroup.id}`
        } else {
          showAlert(true, 'error', t('Something went wrong'))
        }
      })()
    } else if (!token) {
      showAlert(true, 'error', t('You need to log in again'))
    }
  }

  if (!next) {
    return (
      <div className='createGroup'>
        {!isCopy && (
          <Button onClick={handleClickOpen}>{t('CREATE GROUP')}</Button>
        )}
        {((open && isCopy && copyGroup.name) || !isCopy) && (
          <Modal
            open={open}
            onClose={handleClose}
            title={t('Create group')}
            className='createGroup__modal'
          >
            <div className='createGroup__modal-content'>
              {isCopy && (
                <p className='createGroup__based'>based on {copyGroup.name}</p>
              )}
              <div className='row createGroup__row createGroup__fileInput'>
                <TextField
                  name='name'
                  label={t('Name')}
                  margin='dense'
                  fullWidth
                  InputLabelProps={{
                    shrink: true
                  }}
                  required
                  variant='outlined'
                  onChange={handleChange}
                />
                <FileInputComponent
                  imagePreview={false}
                  labelText=''
                  labelStyle={{ fontSize: 14 }}
                  multiple={false}
                  callbackFunction={file_arr => {
                    saveFile(file_arr)
                  }}
                  accept='image/jpg,image/png'
                  onChange={handleChange}
                  buttonComponent={
                    <button style={backgroundLoad} type='button'>
                      <AddPhotoAlternateOutlinedIcon />
                    </button>
                  }
                />
              </div>
              <div className='createGroup__row'>
                <TextField
                  name='description'
                  label={t('Description')}
                  multiline
                  required
                  rows={4}
                  fullWidth
                  defaultValue=''
                  variant='outlined'
                  InputLabelProps={{
                    shrink: true
                  }}
                  onChange={handleChange}
                />
              </div>
              <div className='row createGroup__row createGroup__tooltip'>
                <TextField
                  select
                  label={t('Entry mode')}
                  name='entryMode'
                  required
                  value={groupParams.entryMode}
                  onChange={handleChange}
                  margin='dense'
                  variant='outlined'
                  inputProps={{
                    name: 'entryMode'
                  }}
                >
                  {modes.map(mode => (
                    <MenuItem key={mode} value={mode}>
                      {t(mode)}
                    </MenuItem>
                  ))}
                </TextField>
                <Tooltip
                  title={
                    <Fragment>
                      <p>{t('Public tooltip')}</p>
                      <p>{t('Closed tooltip')}</p>
                      <p>{t('Private tooltip')}</p>
                    </Fragment>
                  }
                />
              </div>

              <div className='row createGroup__row createGroup__tooltip'>
                <TextField
                  name='domainRestrictions'
                  label={t('Domain Restriction')}
                  margin='dense'
                  fullWidth
                  value={groupParams.domainRestrictions}
                  InputLabelProps={{
                    shrink: true
                  }}
                  variant='outlined'
                  onChange={handleChange}
                  disabled={me.subscriptionPlan !== 'FREE' ? false : true}
                />
                <Tooltip title='You can restrict access to group by user’s email domain.' />
              </div>

              <div className='row createGroup__row createGroup__tooltip'>
                <TextField
                  name='entryQuestion'
                  label={t('Join request question')}
                  margin='dense'
                  fullWidth
                  value={groupParams.entryQuestion}
                  InputLabelProps={{
                    shrink: true
                  }}
                  variant='outlined'
                  onChange={handleChange}
                />
                <Tooltip title={t('Any information')} />
              </div>
              <ModalButtons
                acceptText={t('Next')}
                handleClose={handleClose}
                handleAccept={handleNextForm}
              />
              {openSnackbar && (
                <ShowAlert
                  open={openSnackbar}
                  message={alertMessage}
                  severity={severity}
                />
              )}
            </div>
          </Modal>
        )}
      </div>
    )
  } else {
    return (
      <>
        <CreateGroupInviteMembers
          handleCreateGroup={handleCreateGroup}
          members={members}
          setMembers={setMembers}
          inviteMessage={inviteMessage}
          setInviteMessage={setInviteMessage}
          setNext={setNext}
          isNew={true}
          groupParams={groupParams}
          setGroupParams={setGroupParams}
        />
        {openSnackbar && (
          <ShowAlert
            open={openSnackbar}
            message={alertMessage}
            severity={severity}
          />
        )}
      </>
    )
  }
}
