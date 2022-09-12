import { useEffect, useState } from 'react'
import { useRecoilValue } from 'recoil'

import { meInfoState, tokenState } from '../../../../../../../state'

// import InfiniteScroll from 'react-infinite-scroll-component'
import { Button, CircularProgress } from '@material-ui/core'
import { GroupMainScreenCardCommentsField } from './GroupMainScreenCardCommentsField/GroupMainScreenCardCommentsField'
import { GroupMainScreenCardCommentsListContent } from './GroupMainScreenCardCommentsListContent/GroupMainCardCommentsListContent'

import { GroupMainScreenIdeaDialogCardCommentsProps } from './GroupMainScreenIdeaDialogCardCommentsInterfaces'
import './GroupMainScreenIdeaDialogCardComments.scss'

import { useTranslation } from 'react-i18next'
import { fetchComments } from '../../../../../../../functionsRequests'
import { findAuthorObj, removeDuplicates } from '../../../../../../../functions'

export const GroupMainScreenIdeaDialogCardComments: React.FC<GroupMainScreenIdeaDialogCardCommentsProps> = (
  props: GroupMainScreenIdeaDialogCardCommentsProps
) => {
  const { ideaId, showAlert, isAdmin, authorIdea } = props

  const me = useRecoilValue(meInfoState)

  const [comments, setComments] = useState({ comments: [], users: [] })
  const [isFetching, setIsFetching] = useState(true)

  const token = useRecoilValue(tokenState)

  const { t } = useTranslation()

  useEffect(() => {
    fetchMoreData(true, comments)
  }, [ideaId])

  const fetchMoreData = (isMore?: boolean, newComments?: any) => {
    if (
      (isFetching && comments.comments.length !== 0) ||
      isMore ||
      newComments
    ) {
      ;(async () => {
        const moreLength =
          isMore && newComments.comments.length !== 0
            ? 10 - (newComments.comments.length % 10)
            : 10

        const curComments = isMore ? newComments : comments

        const newFetchComments =
          isMore !== undefined && comments.comments.length !== 0
            ? { comments: [], users: [] }
            : await fetchComments(
                token,
                ideaId,
                curComments.comments.length,
                moreLength
              )

        if ((await newFetchComments) !== 'undefined') {
          const uniqueComments = removeDuplicates(
            newComments
              ? newComments.comments.concat((await newFetchComments).comments)
              : comments.comments.concat((await newFetchComments).comments)
          )

          const uniqueUsers = removeDuplicates(
            newComments
              ? newComments.users.concat((await newFetchComments).users)
              : comments.users.concat((await newFetchComments).users)
          )

          const newCommentsObj = {
            comments: uniqueComments,
            users: uniqueUsers
          }
          setComments(newCommentsObj)

          //(newCommentsObj.comments.length % 10 !== 0 ||
          if (
            (newFetchComments.comments.length == 0 ||
              newFetchComments.comments.length % 10 !== 0) &&
            isMore == undefined
          ) {
            setIsFetching(false)
          }
        }

        if (!isMore && newComments) {
          setComments(newComments)
        }
      })()
    } else {
      setIsFetching(false)
    }
  }

  return (
    <div className='groupMainScreenIdeaDialogCardComments'>
      <GroupMainScreenCardCommentsField
        ideaId={ideaId}
        token={token}
        showAlert={showAlert}
        fetchMoreData={fetchMoreData}
        comments={comments}
        t={t}
      />
      {Array.isArray(comments.comments) && comments.comments.length > 0 && (
        <div
          id='groupMainScreenCommentsList'
          className='groupMainScreenIdeaDialogCardComments__list'
        >
          {/* <InfiniteScroll
            dataLength={comments.comments.length}
            next={fetchMoreData}
            hasMore={comments.comments.length == 0 ? false : isFetching}
            loader={
              <div className='groupMainScreenIdeaDialogCardComments__progress'>
                <CircularProgress />
              </div>
            }
            scrollableTarget='groupMainScreenCommentsList'
          > */}
          {comments.comments.map(comment => (
            <GroupMainScreenCardCommentsListContent
              key={comment.id}
              ideaId={ideaId}
              token={token}
              comment={comment}
              author={findAuthorObj(comment.author, comments.users, me)}
              setComments={setComments}
              comments={comments}
              fetchMoreData={fetchMoreData}
              showAlert={showAlert}
              t={t}
              isOptions={isAdmin || comment.author == me.sub}
            />
          ))}
          {isFetching && (
            <div className='groupMainScreenIdeaDialogCardComments__progress'>
              <Button
                onClick={() => fetchMoreData()}
                color='primary'
                className='groupMainScreenIdeaDialogCardComments__more'
              >
                {t('Read more')}
              </Button>
            </div>
          )}
          {/* </InfiniteScroll> */}
        </div>
      )}
    </div>
  )
}
