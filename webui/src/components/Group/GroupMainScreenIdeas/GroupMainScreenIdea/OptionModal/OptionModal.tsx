import { useEffect, useState } from 'react'
import { useRecoilValue } from 'recoil'
import { Snackbar } from '@material-ui/core'
import { useTranslation } from 'react-i18next'
import {
  patchAssignee,
  patchMoveToGroup
} from '../../../../../functionsRequests'
import { tokenState, authorsNamesState } from '../../../../../state'

import { SearchShortList } from '../../../SearchShortList/SearchShortList'
import { Alert } from '../../../../Alert/Alert'
import { Modal } from '../../../../Modal/Modal'

import './OptionModal.scss'
import { findAssignee} from '../../../../../functions'
import { ModalButtons } from '../../../../Modal/ModalButtons/ModalButtons'

const handleCloseSnackbar = (event?: React.SyntheticEvent, reason?: string) => {
  if (reason === 'clickaway') {
    return
  }
}

export const OptionModal = props => {
  const { handleClose, idea, setIdea, param, setIdeas, ideas, openId, members } = props

  const [curMembers, setCurMembers] = useState([])

  const item = param == 'change' ? idea.assignee : idea.groupId
  const token = useRecoilValue(tokenState)
  const [changedItem, setChangedItem] = useState(item)

  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')

  const [openSnackbar, setOpenSnackbar] = useState(false)
  const authorsNames = useRecoilValue(authorsNamesState)
  const author = findAssignee(idea, authorsNames)

  const { t } = useTranslation()

  const showAlert = (open, severity, message) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)
  }

  useEffect(() => {
    if (param == 'change') {
      setCurMembers(members)
    }
  }, [])

  useEffect(() => {}, author)

  const handleChange = e => {
    const ideaId = idea.id
    const responseBody =
      param == 'change' ? { userId: changedItem } : { groupId: changedItem }
    const body = JSON.stringify(responseBody)

    if (ideaId) {
      if (param == 'change') {
        patchAssignee(token, ideaId, body).then(res => {
          if (res !== undefined && res !== 'undefined') {
            showAlert(true, 'success', t('Assignee changed successfully'))
            setIdea(res)
            setTimeout(() => {handleClose(e)}, 2000)
          } else {
            showAlert(true, 'error', t('Assignee cannot be changed'))
          }
        })
      }
      if (param == 'move') {
        if (patchMoveToGroup(token, ideaId, body)) {
          let newIdeas = Object.assign({}, ideas)
          newIdeas = ideas.filter(idea => idea.id !== ideaId)
          setIdeas(newIdeas)

          if (openId) {
            window.location.href = `/group/${changedItem}`
          }
          showAlert(true, 'success', t('Idea moved successfully'))
        } else {
          showAlert(true, 'error', t('Something went wrong'))
        }

        handleClose(e)
      }
    }
  }

  const modalTitle = param == 'change' ? t('Change Assignee') : t('Move to group')

  return (
    <>
      <Modal
        open={true}
        onClose={handleClose}
        title={modalTitle}
        className='optionModal'
      >
        {param == 'change' && (
          <p id='simple-modal-description'>
            {item && `${t('now assigned to')} ${author}`}
            {!item && t(`Idea is not assigned to anybody`)}
          </p>
        )}
        <SearchShortList
          changedItem={changedItem}
          setChangedItem={setChangedItem}
          param={param}
          curMembers={curMembers}
        />
        <ModalButtons
          acceptText={param == 'change' ? t('CHANGE') : t('MOVE')}
          handleClose={handleClose}
          handleAccept={handleChange}
        />
      </Modal>
      <Snackbar
        open={openSnackbar}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
      >
        <Alert onClose={handleCloseSnackbar} severity={severity}>
          {alertMessage}
        </Alert>
      </Snackbar>
    </>
  )
}
