import { useEffect, useRef, useState } from 'react'
import { useRecoilValue } from 'recoil'

import {
  Comment as CommentIcon,
  ThumbUp as ThumbUpIcon,
  Close as CloseIcon
} from '@material-ui/icons'
import { CardContent, CardActions, IconButton, Button } from '@material-ui/core'
import { GroupMainScreenOptions } from '../../../GroupMainScreen/GroupMainScreenOptions/GroupMainScreenOptions'
import { EditorState, convertFromRaw } from 'draft-js'

import { Editor } from 'react-draft-wysiwyg'
import { markdownToDraft } from 'markdown-draft-js'

import { meInfoState } from '../../../../../state'
import { GroupMainScreenCardContentProps } from './GroupMainScreenCardContentInterfaces'
import { findAuthorForIdea, isJson } from '../../../../../functions'

import { useTranslation } from 'react-i18next'

import './GroupMainScreenIdeaCardContent.scss'

export const GroupMainScreenIdeaCardContent: React.FC<GroupMainScreenCardContentProps> = (
  props: GroupMainScreenCardContentProps
) => {
  const {
    idea,
    allIdeas,
    handleClickAssignee,
    handleOptionGroup,
    handleClickVote,
    handleOpenModal,
    assignee,
    votes,
    options,
    isExtendedMode
  } = props

  const [localAssignee, setLocalAssignee] = useState(assignee)
  const [commentsCounts, setCommentsCounts] = useState(+(allIdeas.commentsCounts[idea.id] || 0))

  const [editorState, setEditorState] = useState(
    isJson(idea.description)
      ? EditorState.createWithContent(
          convertFromRaw(JSON.parse(idea.description))
        )
      : EditorState.createWithContent(
          convertFromRaw(markdownToDraft(idea.description))
        )
  )

  const anchorRef = useRef(null)

  const me = useRecoilValue(meInfoState)

  const { t } = useTranslation()

  const isMeVoted = idea.voters.find(vote => vote === me.sub)

  const handleChangeEditor = () => {}

  useEffect(() => {
    const newAssignee = findAuthorForIdea(idea, allIdeas, me, 'assignee')
    if (newAssignee) {
      setLocalAssignee(newAssignee)
    }
    setEditorState(
      isJson(idea.description)
        ? EditorState.createWithContent(
            convertFromRaw(JSON.parse(idea.description))
          )
        : EditorState.createWithContent(
            convertFromRaw(markdownToDraft(idea.description))
          )
    )
  }, [idea])

  // useEffect(() => {
  //   setCommentsCounts(+(allIdeas.commentsCounts[idea.id] || 0))
  // console.log(allIdeas.commentsCounts[idea.id], idea.id)
  // })

  return (
    <div className='groupMainScreenIdeaCardContent'>
      <CardContent onClick={handleOpenModal}>
        <Editor
          toolbarHidden
          editorState={editorState}
          onChange={handleChangeEditor}
        />
        {idea.link && (
          <a href={idea.link} className='groupMainScreenIdeaCardContent__link'>
            {idea.link}
          </a>
        )}
      </CardContent>
      <CardActions disableSpacing>
        {!idea.assignee && !idea.archived && !idea.implemented && (
          <Button
            onClick={handleClickAssignee}
            className='groupMainScreenIdeaCardContent__assigneeBtn'
            variant='contained'
            color='primary'
          >
            {t('ASSIGN TO ME')}
          </Button>
        )}
        {me.sub == idea.assignee && !idea.archived && !idea.implemented && (
          <div
            className='groupMainScreenIdeaCardContent__assignee'
            onClick={handleClickAssignee}
          >
            <p>{t('Assigned to you')}</p>
            <CloseIcon />
          </div>
        )}
        {me.sub !== idea.assignee &&
          idea.assignee &&
          !idea.archived &&
          !idea.implemented && (
            <div
              className='groupMainScreenIdeaCardContent__assignee'
              onClick={handleClickAssignee}
            >
              <p>
                {t('Assigned to')} {localAssignee}
              </p>
              <CloseIcon />
            </div>
          )}
        {!isExtendedMode && (
          <>
            {/* <IconButton
              className='groupMainScreenIdeaCardContent__button'
              aria-label='comments'
            > */}
              <CommentIcon className='groupMainScreenIdeaCardContent__button groupMainScreenIdeaCardContent__button--comment'/>
            {/* </IconButton> */}

            <span className='groupMainScreenIdeaCardContent__comments'>
              {commentsCounts}
            </span>
          </>
        )}
        {!isExtendedMode && (
          <>
            <IconButton
              className={
                isMeVoted ? 'groupMainScreenIdeaCardContent__vote--active' : ''
              }
              onClick={() => {
                if (!idea.archived && !idea.implemented) handleClickVote()
              }}
              aria-label='vote'
            >
              <ThumbUpIcon />
            </IconButton>
            <span className='groupMainScreenIdeaCardContent__votes'>
              {votes}
            </span>
          </>
        )}
        {isExtendedMode && (
          <div
            className={
              isMeVoted
                ? 'groupMainScreenIdeaCardContent__votes--voted'
                : 'groupMainScreenIdeaCardContent__votes--extended'
            }
          >
            <span className='groupMainScreenIdeaCardContent__votes'>
              {votes
                ? `${t('already has votes')} ${votes}`
                : t('Be the first who')}
            </span>
            <IconButton
              className={
                isMeVoted
                  ? 'groupMainScreenIdeaCardContent__vote--voted'
                  : 'groupMainScreenIdeaCardContent__vote--extended'
              }
              onClick={() => {
                if (!idea.archived && !idea.implemented) handleClickVote()
              }}
              aria-label='vote'
            >
              {' '}
              {!isMeVoted && (
                <>
                  <ThumbUpIcon className='groupMainScreenIdeaCardContent__vote-icon' />{' '}
                  {t('VOTE')}
                </>
              )}
              {isMeVoted && t('UN VOTE')}
            </IconButton>
          </div>
        )}
        {options && (
          <GroupMainScreenOptions
            options={options}
            id={`more${idea.id}`}
            handleOption={handleOptionGroup}
            idea={idea}
            anchorRef={anchorRef}
          />
        )}
      </CardActions>
    </div>
  )
}
