import { useEffect, useState } from 'react'
import { useRecoilValue } from 'recoil'
import { useHistory } from 'react-router'

import { InsertLink as InsertLinkIcon } from '@material-ui/icons'
import { Card, CardHeader, IconButton, Button } from '@material-ui/core'
import { CopyToClipboard } from 'react-copy-to-clipboard'

import { currentGroupState, meInfoState } from '../../../../../../state'

import { useTranslation } from 'react-i18next'

import { CreateIdea } from '../../../../CreateIdea/CreateIdea'
import { GroupMainScreenIdeaDialogCardComments } from './GroupMainScreenIdeaDialogCardComments/GroupMainScreenIdeaDialogCardComments'
import { GroupMainScreenIdeaCardContent } from '../../GroupMainScreenIdeaCardContent/GroupMainScreenIdeaCardContent'

import { GroupMainScreenIdeaDialogCardProps } from './GroupMainScreenIdeaDialogCardInterfaces'

import './GroupMainScreenIdeaDialogCard.scss'
import { createListOptions } from '../../../../../../functions'

export const GroupMainScreenIdeaDialogCard: React.FC<GroupMainScreenIdeaDialogCardProps> = (
  props: GroupMainScreenIdeaDialogCardProps
) => {
  const {
    idea,
    assignee,
    convertDate,
    author,
    edit,
    setEdit,
    handleEdit,
    setNewIdea,
    setOpenDialog,
    handleClickAssignee,
    handleOptionGroup,
    votes,
    handleClickVote,
    openId,
    showAlert,
    isAdmin,
    allIdeas,
    isEditAccess,
    fetchMoreData
  } = props

  const { t } = useTranslation()

  const groupId = useRecoilValue(currentGroupState).id
  const me = useRecoilValue(meInfoState)
  const [options, setOptions] = useState([])

  const history = useHistory()

  useEffect(() => {
    setOptions(createListOptions(isAdmin, me, idea))
  }, [idea])

  useEffect(() => {
    if (openId) {
      setOpenDialog(true)
    }
  }, [openId, edit])

  const handleCloseDialog = () => {
    setEdit(false)
    setOpenDialog(false)
  }

  const handleClose = () => {
    setOpenDialog(false)
    setEdit(false)
    history.push(`/group/${idea.groupId}`)
  }

  const handleCopy = () => {
    showAlert(true, 'success', t('Link copied to clipboard'))
  }

  return (
    <div className='groupMainScreenIdeaDialogCard'>
      <Card className='groupMainScreenIdea__card, groupMainScreenIdeaDialogCard__card'>
        <CardHeader
          action={
            <div className='groupMainScreenIdeaDialogCard__actions'>
              <CopyToClipboard
                text={`https://${document.domain}/ideas/${idea.id}`}
              >
                <IconButton onClick={handleCopy} aria-label='settings'>
                  <InsertLinkIcon />
                </IconButton>
              </CopyToClipboard>
              {isEditAccess && (
                <Button onClick={handleEdit}>{t('EDIT')}</Button>
              )}
              <Button onClick={handleClose}>{t('CLOSE')}</Button>
            </div>
          }
          title={idea.summary}
          subheader={
            idea.archived
              ? `Idea is archived`
              : idea.implemented
              ? `Idea is implemented`
              : `${t('offered by')} ${author}, ${convertDate}`
          }
        />
        <GroupMainScreenIdeaCardContent
          idea={idea}
          handleClickAssignee={handleClickAssignee}
          handleOptionGroup={handleOptionGroup}
          handleClickVote={handleClickVote}
          assignee={assignee}
          votes={votes}
          options={options}
          isExtendedMode={true}
          allIdeas={allIdeas}
        />
        <GroupMainScreenIdeaDialogCardComments />
      </Card>
      {edit && (
        <CreateIdea
          setNewIdea={setNewIdea}
          groupId={groupId}
          param={edit}
          ideaEdit={idea}
          handleCloseDialog={handleCloseDialog}
          fetchMoreData={fetchMoreData}
          ideas={allIdeas}
        />
      )}
    </div>
  )
}
