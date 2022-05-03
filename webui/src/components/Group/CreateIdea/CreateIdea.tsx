import { useState, useEffect } from 'react'
import { useRecoilValue } from 'recoil'

import { ShowAlert } from '../../Alert/Alert'
import { TextField, IconButton } from '@material-ui/core/'
import { Add as AddIcon } from '@material-ui/icons/'
import { Modal } from '../../Modal/Modal'
import { ModalButtons } from '../../Modal/ModalButtons/ModalButtons'
import EditorDraft from '../../EditorDraft/EditorDraft3.jsx'

import { postNewIdea } from '../../../functionsRequests'
import { validURL } from '../../../functions'
import { tokenState } from './../../../state'
import { CreateIdeaProps } from './CreateIdeaInterfaces'

import './CreateIdea.scss'

import { useTranslation } from 'react-i18next'

export const CreateIdea = (props: CreateIdeaProps) => {
  const {
    groupId,
    setNewIdea,
    param,
    handleCloseDialog,
    fetchMoreData,
    ideas
  } = props

  const ideaDefault = {
    id: null,
    groupId: groupId,
    summary: '',
    description: '',
    descriptionPlainText: '',
    link: ''
  }

  const ideaEdit = props.ideaEdit || ideaDefault

  const token: any = useRecoilValue(tokenState)
  const [open, setOpen] = useState(false)
  const [ideaParams, setIdeaParams] = useState(ideaEdit)

  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')
  const [openSnackbar, setOpenSnackbar] = useState(false)

  const { t } = useTranslation()

  const showAlert = (open: boolean, severity: string, message: string) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)
    setTimeout(() => setOpenSnackbar(false), 1000)
  }

  useEffect(() => {
    if (param == 'EDIT') {
      setOpen(true)
    }
  }, [param])

  const isValidate = params => {
    if (params.summary.length < 3) {
      showAlert(true, 'error', t('An idea must have a name'))
      return false
    }

    if (params.description.length < 3) {
      showAlert(true, 'error', t('An idea must have a description'))
      return false
    }

    if (params.link && !validURL(params.link)) {
      showAlert(true, 'error', t('The link is not valid'))
      return false
    }

    return true
  }

  const createNewIdea = () => {
    let method = 'POST'
    let url = `https://api.test.saedi.io/ideas`

    if (isValidate(ideaParams)) {
      if (param == 'EDIT') {
        method = 'PATCH'
        url = `https://api.test.saedi.io/ideas/${ideaEdit.id}`
      }

      ;(async () => {
        const newIdea = await postNewIdea(token, method, url, ideaParams)
        if (await newIdea) {
          const successString =
            param == 'EDIT'
              ? t('Idea edited successfully')
              : t('Idea added successfully')
          showAlert(true, 'success', successString)
          setIdeaParams(ideaEdit)

          if (param == 'EDIT') {
            setNewIdea(await newIdea.idea)
            const newIdeas = ideas.ideas.map(idea => {
              return idea.id == newIdea.idea.id ? newIdea.idea : idea
            })
            fetchMoreData(false, newIdeas)
            handleCloseDialog()
          } else {
            fetchMoreData(
              true,
              ideas.ideas.length
                ? [newIdea.idea, ...ideas.ideas]
                : [newIdea.idea]
            )
            setTimeout(() => setOpen(false), 1000)
          }
        } else {
          showAlert(true, 'error', t('Something went wrong'))
          setTimeout(() => setOpen(false), 1000)
        }
      })()
    } else if (!token) {
      showAlert(true, 'error', t('You need to log in again'))
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let { name, value } = e.target

    setIdeaParams(prevData => {
      return {
        ...prevData,
        [name]: value
      }
    })
  }

  const handleClickOpen = () => {
    setOpen(true)
  }

  const handleClose = () => {
    if (param == 'CREATE') {
      setIdeaParams(ideaDefault)
      setOpen(false)
    } else {
      handleCloseDialog()
    }
  }

  return (
    <div className='createIdea'>
      {param !== 'EDIT' && (
        <IconButton
          className='createIdea__button'
          aria-controls='more'
          aria-haspopup='true'
          onClick={handleClickOpen}
        >
          <AddIcon />
        </IconButton>
      )}
      {open && (
        <Modal
          open={open}
          onClose={handleClose}
          title={param == 'EDIT' ? t('Edit idea') : t('Add idea')}
          className='createIdea__modal'
        >
          <div className='createIdea__content'>
            <TextField
              name='summary'
              label={t('Summary')}
              margin='dense'
              InputLabelProps={{
                shrink: true
              }}
              required
              className='createIdea__summary'
              variant='outlined'
              onChange={handleChange}
              value={ideaParams.summary}
            />

            <div className='MuiFormControl-root MuiTextField-root MuiFormControl-marginDense'>
              <label
                className='MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-marginDense MuiInputLabel-outlined Mui-required Mui-required createIdea__description-label'
                data-shrink='true'
              >
                {t('Description')}
                <span
                  aria-hidden='true'
                  className='MuiFormLabel-asterisk MuiInputLabel-asterisk'
                >
                   *
                </span>
              </label>
              <div className='MuiInputBase-root MuiOutlinedInput-root MuiInputBase-formControl MuiInputBase-marginDense MuiOutlinedInput-marginDense'>
                <EditorDraft
                  ideaParams={ideaParams}
                  setIdeaParams={setIdeaParams}
                  groupId={groupId}
                />
              </div>
            </div>
            <TextField
              name='link'
              label={t('URL')}
              margin='dense'
              fullWidth
              InputLabelProps={{
                shrink: true
              }}
              variant='outlined'
              helperText={t('Optional')}
              value={ideaParams.link}
              onChange={handleChange}
            />
            <ModalButtons
              acceptText={param == 'CREATE' ? t('ADD IDEA') : t('SAVE IDEA')}
              handleClose={handleClose}
              handleAccept={createNewIdea}
            />
            {openSnackbar && (
              <ShowAlert
                open={openSnackbar}
                severity={severity}
                message={alertMessage}
              />
            )}
          </div>
        </Modal>
      )}
    </div>
  )
}
