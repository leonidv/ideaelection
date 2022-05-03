import { useRecoilValue } from 'recoil'
import { Avatar, TextField } from '@material-ui/core'
import { meInfoState } from '../../../../../../../state'
import './GroupMainScreenIdeaDialogCardComments.scss'

import { useTranslation } from 'react-i18next'

export const GroupMainScreenIdeaDialogCardComments = () => {
  const me = useRecoilValue(meInfoState)

  const { t } = useTranslation()

  const handleChangeComment = e => {
    return e.target.value
  }

  return (
    <div className='groupMainScreenIdeaDialogCardComments'>
      <div className='groupMainScreenCardDialogComments__first row'>
        <Avatar
          className='groupMainScreenCardDialogComments__avatar'
          alt='avatar'
          src={me.avatar}
        />
        <TextField
          placeholder={t('Add a comment...')}
          onChange={handleChangeComment}
        />
      </div>
    </div>
  )
}
