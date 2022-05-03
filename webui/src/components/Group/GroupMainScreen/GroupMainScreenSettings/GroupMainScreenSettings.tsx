import { useState, useEffect, Fragment } from 'react'
import { useHistory } from 'react-router'
import { useRecoilState, useRecoilValue } from 'recoil'
import { useTranslation } from 'react-i18next'
import copy from 'copy-to-clipboard'

import {
  TextField,
  Button,
  MenuItem,
  IconButton,
  Snackbar
} from '@material-ui/core/'
import {
  AddPhotoAlternateOutlined as AddPhotoAlternateOutlinedIcon,
  Autorenew as AutorenewIcon
} from '@material-ui/icons/'
import FileInputComponent from 'react-file-input-previews-base64'
import { CopyToClipboard } from 'react-copy-to-clipboard'

import { Tooltip } from '../../../Tooltip/Tooltip'
import { Modal } from '../../../Modal/Modal'
import { ModalButtons } from '../../../Modal/ModalButtons/ModalButtons'
import { Alert } from '../../../Alert/Alert'

import { GroupMainScreenSettingsProps } from './GroupMainScreenSettingsInterfaces'

import {
  deleteGroup,
  patchGroup,
  updateJoiningKey
} from '../../../../functionsRequests'
import { isValidateGroupParams, modes } from '../../../../functions'
import {
  tokenState,
  currentGroupState,
  allUserGroupsState,
  meInfoState
} from '../../../../state'

import './GroupMainScreenSettings.scss'

export const GroupMainScreenSettings: React.FC<GroupMainScreenSettingsProps> = (
  props: GroupMainScreenSettingsProps
) => {
  const {
    openGroupMainScreenSettings,
    setOpenGroupMainScreenSettings,
    handleClose
  } = props

  const token: any = useRecoilValue(tokenState)
  const me = useRecoilValue(meInfoState)
  const [group, setGroup] = useRecoilState(currentGroupState)
  const [allGroups, setAllGroups] = useRecoilState(allUserGroupsState)
  const [administrator, setAdministrator] = useState(false)
  const [groupParams, setGroupParams]: any = useState({ group })
  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')
  const [openSnackbar, setOpenSnackbar] = useState(false)

  const [backgroundLoad, setBackgroundLoad]: any = useState({})
  const [isOpenDeleteModal, setIsOpenDeleteModal] = useState(false)

  const history = useHistory()
  const { t } = useTranslation()

  useEffect(() => {
    setGroupParams(group)
    setBackgroundLoad({ backgroundImage: `url("${group.logo}")` })
    setAdministrator(
      group && group.creator && group.creator.id == me.sub ? true : false
    )
  }, [group])

  useEffect(() => {}, [groupParams, openSnackbar])

  const saveFile = file => {
    setBackgroundLoad({ backgroundImage: `url("${file.base64}")` })
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
    if (administrator) {
      setGroupParams(prevData => {
        return {
          ...prevData,
          [name]: value
        }
      })
    }
  }

  const showAlert = (open, severity, message) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)

    setTimeout(() => {
      setOpenSnackbar(false)
    }, 2000)
  }

  const handleUpdateGroup = e => {
    if (isValidateGroupParams(groupParams, showAlert)) {
      patchGroup(token, groupParams, group.id).then(res => {
        if (res !== 'undefined') {
          showAlert(true, 'success', t(`Group successfully changed`))

          setTimeout(() => {
            setAllGroups(
              allGroups.map(group => {
                return group.id == res.id ? res : group
              })
            )

            setGroup(res)
            handleClose()
          }, 2000)
        } else if (res == 'undefined') {
          showAlert(true, 'error', t(`You're haven't permissions`))
        }
      })
      //setOpenSnackbar(false)
    } else if (!token) {
      showAlert(
        true,
        'error',
        t('You need to log in again') + ' ' + t("or you're haven't permissions")
      )
    }
  }

  const handleDeleteGroup = () => {
    const groupId = group.id

    if (groupId) {
      ;(async () => {
        if (await deleteGroup(token, groupId)) {
          setOpenGroupMainScreenSettings(false)
          setAllGroups(allGroups.filter(group => group.id !== groupId))
          handleCloseDeleteModal()
          history.push('/main')
        }
      })()
    }
  }

  const handleOpenDeleteModal = () => {
    setIsOpenDeleteModal(true)
  }

  const handleCloseDeleteModal = () => {
    setIsOpenDeleteModal(false)
  }

  const handleUpdateLink = () => {
    updateJoiningKey(token, group.id).then(res => {
      if (res) {
        setGroupParams({ ...groupParams, joiningKey: res.joiningKey })
        showAlert(true, 'success', t('Link updated successfully'))
      } else {
        showAlert(true, 'error', t('Something went wrong'))
      }
    })
  }

  const handleCopyLink = () => {
    showAlert(true, 'success', t('Link copied successfully'))
    copy(
      `${window.location.protocol}//${window.location.hostname}/groups/inviteLink=${groupParams.joiningKey}`
    )
  }

  return (
    <div className='groupMainScreenSettings'>
      <Modal
        open={openGroupMainScreenSettings}
        onClose={handleClose}
        className='groupMainScreenSettings__modal'
        title={t('Group settings')}
      >
        <div className='groupMainScreenSettings__modal-content'>
          <div className='row groupMainScreenSettings__row groupMainScreenSettings__fileInput'>
            <TextField
              disabled={!administrator}
              name='name'
              label={t('Name')}
              margin='dense'
              fullWidth
              value={groupParams.name}
              InputLabelProps={{
                shrink: true
              }}
              required
              variant='outlined'
              onChange={handleChange}
            />
            <FileInputComponent
              disabled={!administrator}
              imagePreview={false}
              labelText=''
              labelStyle={{ fontSize: 14 }}
              multiple={false}
              callbackFunction={file_arr => {
                saveFile(file_arr)
              }}
              required
              accept='image/jpg,image/png'
              onChange={handleChange}
              buttonComponent={
                <button style={backgroundLoad} type='button'>
                  <AddPhotoAlternateOutlinedIcon />
                </button>
              }
            />
          </div>
          <div className='groupMainScreenSettings__row'>
            <TextField
              disabled={!administrator}
              name='description'
              label={t('Description')}
              multiline
              rows={4}
              required
              fullWidth
              defaultValue=''
              value={groupParams.description}
              variant='outlined'
              InputLabelProps={{
                shrink: true
              }}
              onChange={handleChange}
            />
          </div>
          <div className='row groupMainScreenSettings__row groupMainScreenSettings__tooltip'>
            <TextField
              select
              disabled={!administrator}
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
          <div className='row __row groupMainScreenSettings__tooltip'>
            <TextField
              disabled={!administrator}
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
            />
            <Tooltip title={t('You can restrict access')} />
          </div>
          <div className='row groupMainScreenSettings__row groupMainScreenSettings__tooltip'>
            <TextField
              disabled={!administrator}
              name='entryQuestion'
              label={t('Join request question')}
              fullWidth
              margin='dense'
              value={groupParams.entryQuestion}
              InputLabelProps={{
                shrink: true
              }}
              variant='outlined'
              onChange={handleChange}
            />
            <Tooltip title={t('Any information')} />
          </div>
          <div
            className='row groupMainScreenSettings__row groupMainScreenSettings__tooltip'
          >
            <CopyToClipboard
              onCopy={handleCopyLink}
              text={`${window.location.protocol}//${window.location.hostname}/groups/inviteLink=${groupParams.joiningKey}`}
            >
              <TextField
                className='groupMainScreenSettings__inviteLink'
                name='inviteLink'
                label={t('Invite link')}
                margin='dense'
                fullWidth
                value={`${window.location.hostname}/groups/inviteLink=${groupParams.joiningKey}`}
                InputLabelProps={{
                  shrink: true
                }}
                InputProps={{
                  readOnly: true
                }}
                variant='outlined'
              />
            </CopyToClipboard>

            {administrator && (
              <IconButton
                onClick={handleUpdateLink}
                className='groupMainScreenSettings__update-btn'
              >
                <AutorenewIcon />
              </IconButton>
            )}
            {/* <Tooltip title={t('Any information')} /> */}
          </div>
        </div>

        <div className='groupMainScreenSettings__actions'>
          {administrator && (
            <Button
              onClick={handleOpenDeleteModal}
              className='groupMainScreenSettings__delete'
              variant='contained'
              color='secondary'
            >
              {t('DELETE')}
            </Button>
          )}
          <Button
            className='groupMainScreenSettings__cancel'
            onClick={handleClose}
          >
            {t('Cancel')}
          </Button>
          {administrator && (
            <Button
              onClick={handleUpdateGroup}
              variant='contained'
              color='primary'
              className='groupMainScreenSettings__update-btn'
            >
              {t('UPDATE')}
            </Button>
          )}
        </div>
        {openSnackbar && (
          <Snackbar open={openSnackbar}>
            <Alert severity={severity}>{alertMessage}</Alert>
          </Snackbar>
        )}
      </Modal>
      {isOpenDeleteModal && (
        <Modal
          open={isOpenDeleteModal}
          onClose={handleCloseDeleteModal}
          title={t('Delete group?')}
          className='groupMainScreenSettings__modal'
        >
          <p className='groupMainScreenSettings__text'>
            {t('This action delete your group with all ideas and comments.')}
          </p>

          <ModalButtons
            acceptText='DELETE'
            handleClose={handleCloseDeleteModal}
            handleAccept={handleDeleteGroup}
          />
        </Modal>
      )}
    </div>
  )
}
