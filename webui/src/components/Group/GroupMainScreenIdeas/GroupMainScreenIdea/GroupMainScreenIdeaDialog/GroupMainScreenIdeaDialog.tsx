import { useEffect, useState } from 'react'
import { useHistory } from 'react-router'

import IconButton from '@material-ui/core/IconButton'
import InsertLinkIcon from '@material-ui/icons/InsertLink'
import Button from '@material-ui/core/Button'
import { CopyToClipboard } from 'react-copy-to-clipboard'

import { GroupMainScreenIdeaDialogCard } from './GroupMainScreenIdeaDialogCard/GroupMainScreenIdeaDialogCard'
import { Modal } from '../../../../Modal/Modal'

import { useTranslation } from 'react-i18next'

import { GroupMainScreenIdeaDialogProps } from './GroupMainScreenIdeaDialogInterfaces'

import './GroupMainScreenIdeaDialog.scss'
import { useRecoilValue } from 'recoil'
import { infoMeAtomState } from '../../../../../state'

export const GroupMainScreenIdeaDialog: React.FC<GroupMainScreenIdeaDialogProps> = (
  props: GroupMainScreenIdeaDialogProps
) => {
  const {
    idea,
    author,
    convertDate,
    setNewIdea,
    handleClickAssignee,
    handleOptionGroup,
    handleClickVote,
    votes,
    openId,
    assignee,
    showAlert,
    isAdmin,
    allIdeas,
    fetchMoreData
  } = props

  const [openDialog, setOpenDialog]: any = useState(false)
  const [edit, setEdit] = useState('')
  const [isEditAccess, setIsEditAccess] = useState(false)

  const { t } = useTranslation()
  const history = useHistory()

  const me = useRecoilValue(infoMeAtomState)

  useEffect(() => {
    if (idea.id == openId) {
      setOpenDialog(true)
      document.title = idea.summary
    }

    if (
      isAdmin ||
      (!idea.implemented &&
        (me.sub == idea.assignee || (!idea.assignee && idea.author == me.sub)))
    ) {
      setIsEditAccess(true)
    } else if (isEditAccess) {
      setIsEditAccess(false)
    }
  }, [openId, Object.values(idea), edit])

  const handleEdit = () => {
    setEdit('EDIT')
    setOpenDialog(true)
  }

  const handleClose = () => {
    document.title = 'Saedi'
    setOpenDialog(false)
    history.push(`/group/${idea.groupId}`)
  }

  const handleCopy = () => {
    showAlert(true, 'success', t('Link copied to clipboard'))
  }

  return (
    <div className='groupMainScreenIdeaDialog'>
      <CopyToClipboard text={`https://${document.domain}/ideas/${idea.id}`}>
        <IconButton onClick={handleCopy} aria-label='settings'>
          <InsertLinkIcon />
        </IconButton>
      </CopyToClipboard>
      {isEditAccess && <Button onClick={handleEdit}>{t('EDIT')}</Button>}
      <Modal
        open={openDialog}
        onClose={handleClose}
        className='groupMainScreenIdeaDialog__modal'
      >
        <GroupMainScreenIdeaDialogCard
          isAdmin={isAdmin}
          idea={idea}
          convertDate={convertDate}
          author={author}
          assignee={assignee}
          handleEdit={handleEdit}
          setEdit={setEdit}
          edit={edit}
          setNewIdea={setNewIdea}
          setOpenDialog={setOpenDialog}
          handleClickAssignee={handleClickAssignee}
          handleOptionGroup={handleOptionGroup}
          handleClickVote={handleClickVote}
          votes={votes}
          openId={openId}
          showAlert={showAlert}
          allIdeas={allIdeas}
          isEditAccess={isEditAccess}
          fetchMoreData={fetchMoreData}
        />
      </Modal>
    </div>
  )
}
