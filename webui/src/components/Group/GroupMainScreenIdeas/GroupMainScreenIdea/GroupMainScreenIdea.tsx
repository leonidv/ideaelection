import { useState, useEffect } from 'react'
import { useRecoilState, useRecoilValue } from 'recoil'
import { useHistory } from 'react-router'

import { Card, CardHeader } from '@material-ui/core'
import { OptionModal } from './OptionModal/OptionModal'
import { ShowAlert } from '../../../Alert/Alert'
import { GroupMainScreenIdeaDialog } from './GroupMainScreenIdeaDialog/GroupMainScreenIdeaDialog'
import { GroupMainScreenIdeaCardContent } from './GroupMainScreenIdeaCardContent/GroupMainScreenIdeaCardContent'

import {
  patchAssignee,
  deleteIdea,
  patchIdeaArchive,
  patchIdeaImplemented,
  postVote
} from '../../../../functionsRequests'
import { convertDate, createListOptions } from '../../../../functions'
import {
  tokenState,
  currentGroupState,
  meInfoState,
  ideasState
} from '../../../../state'
import { Idea } from '../../../../types/Idea'
import { GroupMainScreenIdeaProps } from './GroupMainScreenIdeaInterfaces'

import { useTranslation } from 'react-i18next'

import './groupMainScreenIdea.scss'

export const GroupMainScreenIdea: React.FC<GroupMainScreenIdeaProps> = (
  props: GroupMainScreenIdeaProps
) => {
  const {
    author,
    assignee,
    openId,
    ideas,
    setIdeas,
    members,
    fetchMoreData
  } = props

  const [idea, setIdea]: any = useState(props.idea)
  const [optionModalId, setOptionModalId] = useState(null)
  const [groupId, setGroupId] = useState(null)
  const [votes, setVotes] = useState(idea.voters ? idea.voters.length : 0)

  const [options, setOptions] = useState([])

  const [openOptionModal, setOpenOptionModal] = useState(false)
  const [paramOptionModal, setParamOptionModal] = useState('change')

  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')
  const [openSnackbar, setOpenSnackbar] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)

  const [allIdeas, setAllIdeas]: any = useRecoilState(ideasState)

  const group = useRecoilValue(currentGroupState)
  const me = useRecoilValue(meInfoState)
  const token = useRecoilValue(tokenState)

  const history = useHistory()
  const { t } = useTranslation()

  useEffect(() => {
    if (group && group.id && group.id !== groupId) {
      setGroupId(group.id)
    }
    if (group && group.creator.id && me && me.sub) {
      if (group.creator.id == me.sub) {
        setIsAdmin(true)
      } else {
        setIsAdmin(false)
      }
    }
  }, [group])

  useEffect(() => {
    setAllIdeas({
      users: allIdeas.users,
      ideas: ideas.map(local => (local.id == idea.id ? idea : local))
    })
    if (group && group.creator.id && me && me.sub) {
      if (group.creator.id == me.sub) {
        setOptions(createListOptions(true, me, idea))
      } else {
        setOptions(createListOptions(false, me, idea))
      }
    }
  }, [openId, idea])

  useEffect(() => {}, [assignee])

  const showAlert = (open, severity, message) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)
  }

  const handleOptionModal = () => {
    if (openOptionModal) {
      setOptionModalId(idea.id)
    }
    setOpenOptionModal(true)
  }

  const handleCloseOptionModal = () => {
    if (openId) {
      history.push(`/group/${idea.groupId}`)
    }
    setOpenOptionModal(false)
    setOptionModalId(null)
  }

  const handleOptionGroup = (e: any, idea: Idea) => {
    const option = e.target.innerText

    const ideaId = idea.id

    switch (option) {
      case t('Change assignee'):
        setParamOptionModal('change')
        handleOptionModal()
        break
      case t('Remove assignee'):
        const responseBody = { userId: '' }
        const body = JSON.stringify(responseBody)

        if (ideaId) {
          ;(async () => {
            const patchedIdea = await patchAssignee(token, ideaId, body)

            if ((await patchedIdea) !== 'undefined') {
              setIdea(await patchedIdea)
              setIdeas(
                ideas.map(localIdea => {
                  return localIdea.id == ideaId ? patchedIdea : localIdea
                })
              )
              setAllIdeas({
                users: allIdeas.users,
                ideas: ideas
              })
              showAlert(true, 'success', t('Assignee removed successfully'))
            } else {
              showAlert(true, 'error', t('Operation is not permitted'))
            }
          })()
        }
        break
      case t('Delete'):
        if (ideaId) {
          ;(async () => {
            const deletedIdea = await deleteIdea(token, ideaId)

            if ((await deletedIdea) !== 'undefined') {
              setIdea(await deletedIdea)
              showAlert(true, 'success', t('Idea deleted successfully'))
            } else {
              showAlert(true, 'error', t('Operation is not permitted'))
            }
          })()
        }
        break
      case t('Archive'):
        if (ideaId) {
          ;(async () => {
            setIdea(await patchIdeaArchive(token, ideaId, true))
            showAlert(true, 'success', t('Idea archived successfully'))
          })()
        }
        break
      case t('Unarchived'):
        if (ideaId) {
          ;(async () => {
            if (idea.archived) {
              setIdea(await patchIdeaArchive(token, ideaId, false))
              showAlert(
                true,
                'success',
                t('Idea marked as undone successfully')
              )
            } else {
              setIdea(await patchIdeaImplemented(token, ideaId, false))
              showAlert(
                true,
                'success',
                t('Idea is marked as not completed successfully')
              )
            }
          })()
        }

        break
      case t('Mark as undone'):
        if (ideaId) {
          ;(async () => {
            setIdea(await patchIdeaImplemented(token, ideaId, false))
            showAlert(
              true,
              'success',
              t('Idea is marked as not completed successfully')
            )
          })()
        }
        break
      case t('Mark as Done'):
        if (ideaId) {
          ;(async () => {
            setIdea(await patchIdeaImplemented(token, ideaId, true))
            showAlert(true, 'success', t('Idea marked as done successfully'))
          })()
        }
        break
      case t('Move to group...'):
        setParamOptionModal('move')
        handleOptionModal()
        break
    }
  }

  const handleClickAssignee = () => {
    const ideaId = idea.id
    const userId = me.sub

    let responseBody: any = { userId: '' }

    if (!idea.assignee) {
      responseBody = { userId: userId }
    }

    const body = JSON.stringify(responseBody)

    if (ideaId) {
      ;(async () => {
        const newAssignee = await patchAssignee(token, ideaId, body)
        if ((await newAssignee) !== 'undefined' && (await newAssignee)) {
          setIdea(await newAssignee)
          showAlert(true, 'success', t('Assignee successfully changed'))
        } else {
          showAlert(true, 'error', t('Operation is not permited'))
        }
      })()
    }
  }

  const handleOpenModal = () => {
    history.push(`/ideas/${idea.id}`)
  }

  const handleClickVote = () => {
    const ideaId = idea.id

    let method = 'POST'

    idea.voters.map(voter => {
      if (me.sub == voter) method = 'DELETE'
    })

    if (ideaId) {
      ;(async () => {
        const ideaVote = await postVote(token, ideaId, method)
        setIdea(await ideaVote)
        setVotes(await ideaVote.voters.length)
      })()
    }
  }

  if (!idea.deleted) {
    return (
      <div className='groupMainScreenIdea'>
        <Card className='groupMainScreenIdea__card'>
          <CardHeader
            action={
              <GroupMainScreenIdeaDialog
                idea={idea}
                assignee={assignee}
                author={author}
                convertDate={convertDate(idea.ctime)}
                setNewIdea={setIdea}
                handleClickAssignee={handleClickAssignee}
                handleOptionGroup={handleOptionGroup}
                handleClickVote={handleClickVote}
                votes={votes}
                openId={openId}
                showAlert={showAlert}
                isAdmin={isAdmin}
                allIdeas={allIdeas}
                fetchMoreData={fetchMoreData}
              />
            }
            title={idea.summary}
            subheader={
              idea.archived
                ? t(`Idea is archived`)
                : idea.implemented
                ? t(`Idea is implemented`)
                : `${t('offered by')} ${author}, ${convertDate(idea.ctime)}`
            }
          />
          <GroupMainScreenIdeaCardContent
            idea={idea}
            handleClickAssignee={handleClickAssignee}
            handleOptionGroup={handleOptionGroup}
            handleClickVote={handleClickVote}
            handleOpenModal={handleOpenModal}
            assignee={assignee}
            votes={votes}
            options={options}
            allIdeas={allIdeas}
          />
        </Card>
        {isAdmin && openOptionModal && optionModalId !== idea.id && (
          <OptionModal
            open={openOptionModal}
            openId={openId}
            handleClose={handleCloseOptionModal}
            param={paramOptionModal}
            idea={idea}
            setIdea={setIdea}
            setIdeas={setIdeas}
            ideas={ideas}
            members={members}
          />
        )}
        {openSnackbar && (
          <ShowAlert
            open={openSnackbar}
            severity={severity}
            message={alertMessage}
          />
        )}
      </div>
    )
  } else {
    return <></>
  }
}
