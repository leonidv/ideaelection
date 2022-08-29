import { useState } from 'react'
import { useRecoilState, useRecoilValue } from 'recoil'
import { Avatar, TextField } from '@material-ui/core'

import { ideasState, meInfoState } from '../../../../../../../../state'
import { editComment, postNewComment } from '../../../../../../../../functionsRequests'

import {
  GroupMainScreenCardCommentsFieldProps,
  defaultCommentContent
} from './GroupMainScreenCardCommentsFieldInterfaces'
import './GroupMainScreenCardCommentsField.scss'

export const GroupMainScreenCardCommentsField: React.FC<GroupMainScreenCardCommentsFieldProps> = (
  props: GroupMainScreenCardCommentsFieldProps
) => {
  const {
    ideaId,
    showAlert,
    token,
    t,
    isReply,
    setIsReply,
    isEdit,
    setIsEdit,
    comments,
    comment,
    setComments,
    setCommentContent,
    fetchMoreData
  } = props
  
  const me = useRecoilValue(meInfoState)
  const [newComment, setNewComment] = useState(isEdit ? comment : defaultCommentContent)
  const [allIdeas, setAllIdeas]: any = useRecoilState(ideasState)

  const handleSaveComment = e => {
    if (e.key === 'Enter') {
      if (newComment.content) {
        postNewComment(token, ideaId, newComment).then(res => {
          if (res) {
            setNewComment({ content: '', replyTo: null })
            if (typeof setIsReply === 'function') {
              setIsReply(false)
            }
            const moreComments = Object.assign({}, comments)
            if (isReply) {
                const indexReply = moreComments.comments.findIndex(el => el.id === res.replyTo) + 1;
                moreComments.comments = [...comments.comments.slice(0,indexReply), res, ...comments.comments.slice(indexReply)]
            } else {
                moreComments.comments = [res, ...comments.comments]
            }
            const counts = +(allIdeas.commentsCounts[ideaId] || 0)
            setAllIdeas({...allIdeas, commentsCounts: {...allIdeas.commentsCounts, [ideaId]: counts + 1 }})
            fetchMoreData(true, moreComments)
          } else {
            showAlert(true, 'error', t('Something went wrong'))
          }
        })
      }
    }
  }

  const handleEditComment = e => {
    if (e.key === 'Enter') {
      if (newComment.content) {
        const newContent = newComment.content
        editComment(token, ideaId, comment.id, {content: newContent}).then(res => {
          if (res) {
            setNewComment({ content: '', replyTo: null })
            if (typeof setIsEdit === 'function') {
              setIsEdit(false)
            }
            const editComments = Object.assign({}, comments)
                const indexReply = editComments.comments.findIndex(el => el.id === comment.id);
                editComments.comments[indexReply].content = newContent
                setComments(editComments)
                setCommentContent(newContent)
          } else {
            showAlert(true, 'error', t('Something went wrong'))
          }
        })
      }
    }
  }

  const handleReplyComment = e => {
    setNewComment({ ...newComment, replyTo: comment.id })
    handleSaveComment(e)
  }

  const handleChangeComment = e => {
    let { name, value } = e.target

    setNewComment(prevData => {
      return {
        ...prevData,
        [name]: value
      }
    })
    return e.target.value
  }

  if (isReply) {
    return (
      <TextField
        name='content'
        className='groupMainScreenCardCommentsField__reply'
        placeholder={t('Add a comment...')}
        value={newComment.content}
        onChange={handleChangeComment}
        onKeyDown={handleReplyComment}
      />
    )
  }

  if (isEdit) {
    return (
        <TextField
          name='content'
          className='groupMainScreenCardCommentsField__edit'
          value={newComment.content}
          onChange={handleChangeComment}
          onKeyDown={handleEditComment}
        />
      )
  }

  return (
    <div className='groupMainScreenCardCommentsField row'>
      <Avatar
        className='groupMainScreenCardCommentsField__avatar'
        alt='avatar'
        src={me.avatar}
      />
      <TextField
        name='content'
        placeholder={t('Add a comment...')}
        value={newComment.content}
        onChange={handleChangeComment}
        onKeyDown={handleSaveComment}
      />
    </div>
  )
}
