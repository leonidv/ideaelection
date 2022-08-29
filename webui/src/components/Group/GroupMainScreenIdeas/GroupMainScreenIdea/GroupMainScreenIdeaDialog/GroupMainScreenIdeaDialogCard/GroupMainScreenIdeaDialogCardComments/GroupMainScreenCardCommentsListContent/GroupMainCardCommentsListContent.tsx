import { useEffect, useRef, useState } from 'react'
import { useRecoilState, useRecoilValue } from 'recoil'

import { Avatar, Button, IconButton } from '@material-ui/core'
import { ThumbUp as ThumbUpIcon } from '@material-ui/icons'

import { GroupMainScreenCardCommentsField } from '../GroupMainScreenCardCommentsField/GroupMainScreenCardCommentsField'
import { GroupMainScreenCardCommentsListContentProps } from './GroupMainCardCommentsListContentInterfaces'
import {
  convertDateWithTime,
  findAuthorObj
} from '../../../../../../../../functions'
import './GroupMainCardCommentsListContent.scss'
import { GroupMainScreenOptions } from '../../../../../../GroupMainScreen/GroupMainScreenOptions/GroupMainScreenOptions'
import { ideasState, meInfoState } from '../../../../../../../../state'
import { deleteComment } from '../../../../../../../../functionsRequests'

export const GroupMainScreenCardCommentsListContent: React.FC<GroupMainScreenCardCommentsListContentProps> = (
  props: GroupMainScreenCardCommentsListContentProps
) => {
  const {
    ideaId,
    comment,
    author,
    t,
    token,
    showAlert,
    comments,
    setComments,
    fetchMoreData,
    isOptions
  } = props

  const me = useRecoilValue(meInfoState)
  const [allIdeas, setAllIdeas]: any = useRecoilState(ideasState)

  const [isReply, setIsReply] = useState(false)
  const [commentContent, setCommentContent] = useState(comment.content)
  const [isEdit, setIsEdit] = useState(false)
  const [replyComment, setReplyComment] = useState({
    id: '',
    content: '',
    ctime: '',
    author: ''
  })
  const [replyAuthor, setReplyAuthor] = useState({
    id: '',
    displayName: ''
  })

  const options = ['Delete', 'Edit']

  const anchorRef = useRef(null)

  useEffect(() => {
    if (comment && comment.replyTo) {
      const foundComment = findAuthorObj(comment.replyTo, comments.comments, me)
      if (foundComment.id) {
        setReplyComment(foundComment)
        setReplyAuthor(findAuthorObj(foundComment.author, comments.users, me))
      }
    }
  }, [comment])

  const handleClickReply = () => {
    setIsReply(!isReply)
  }

  const handleOption = e => {
    const option = e.target.innerText

    switch (option) {
      case t('Delete'):
        deleteComment(token, ideaId, comment.id).then(res => {
          if (res == 'Deleted') {
            const newCommentsList = comments.comments.filter(
              cur => cur.id !== comment.id
            )
            const newCommentsObj = { ...comments, comments: newCommentsList }
            setComments(newCommentsObj)
            const counts = +(allIdeas.commentsCounts[ideaId] || 0)
            setAllIdeas({
              ...allIdeas,
              commentsCounts: {
                ...allIdeas.commentsCounts,
                [ideaId]: counts - 1
              }
            })
            fetchMoreData(true, newCommentsObj)
            showAlert(true, 'success', t('Comment deleted succesfully'))
          } else {
            showAlert(true, 'error', t('Comment cannot be removed'))
          }
        })
        break
      case t('Edit'):
        setIsEdit(!isEdit)
        break
    }
  }

  return (
    <div className='groupMainScreenCardCommentsListContent'>
      <div className='groupMainScreenCardCommentsListContent__row row'>
        <Avatar
          className='groupMainScreenCardCommentsListContent__avatar'
          alt='avatar'
          src={author.avatar}
        />
        <div className='groupMainScreenCardCommentsListContent__col'>
          <div className='groupMainScreenCardCommentsListContent__firstRow row'>
            <span className='groupMainScreenCardCommentsListContent__name'>
              {author.displayName}
            </span>
            <span className='groupMainScreenCardCommentsListContent__time'>
              {convertDateWithTime(comment.ctime)}
            </span>

            <div className='groupMainScreenCardCommentsListContent__buttons'>
              <IconButton
                className='groupMainScreenCardCommentsListContent__vote'
                aria-label='vote'
              >
                <ThumbUpIcon />
              </IconButton>
              <Button
                onClick={handleClickReply}
                className='groupMainScreenCardCommentsListContent__reply'
              >
                {t('REPLY')}
              </Button>
            </div>

            {isOptions && options && (
              <GroupMainScreenOptions
                options={options}
                id={`${comment.id}`}
                handleOption={handleOption}
                anchorRef={anchorRef}
              />
            )}
          </div>

          {replyComment.id && (
            <div className='groupMainScreenCardCommentsListContent__replyCol'>
              <div className='groupMainScreenCardCommentsListContent__firstRow groupMainScreenCardCommentsListContent__replyRow'>
                <span className='groupMainScreenCardCommentsListContent__replyName'>
                  {replyAuthor.displayName}
                </span>
                <span className='groupMainScreenCardCommentsListContent__replyTime'>
                  {convertDateWithTime(replyComment.ctime)}
                </span>
              </div>
              <div className='groupMainScreenCardCommentsListContent__replyContent'>
                {replyComment.content}
              </div>
            </div>
          )}

          {!isEdit && (
            <div className='groupMainScreenCardCommentsListContent__content'>
              {commentContent}
            </div>
          )}

          {isEdit && (
            <GroupMainScreenCardCommentsField
              ideaId={ideaId}
              token={token}
              showAlert={showAlert}
              t={t}
              isEdit={isEdit}
              setIsEdit={setIsEdit}
              comments={comments}
              comment={comment}
              setComments={setComments}
              setCommentContent={setCommentContent}
              fetchMoreData={fetchMoreData}
            />
          )}

          {isReply && (
            <GroupMainScreenCardCommentsField
              ideaId={ideaId}
              token={token}
              showAlert={showAlert}
              t={t}
              isReply={isReply}
              setIsReply={setIsReply}
              comment={comment}
              comments={comments}
              fetchMoreData={fetchMoreData}
            />
          )}
        </div>
      </div>
    </div>
  )
}
