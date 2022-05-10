import { useEffect } from 'react'
import { currentGroupState } from '../../state'
import { useSetRecoilState } from 'recoil'

import { GroupMainScreen } from '../Group/GroupMainScreen/GroupMainScreen'

import { switchParamsOrdering } from '../../functions'
import { defaultGroups } from '../../types/Groups'

import './Invites.scss'

export const Invites = props => {
  const { fetchMoreData, showAlert } = props
  const setGroup = useSetRecoilState(currentGroupState)

  const states = {
    ordering: ''
  }

  useEffect(() => {
    setGroup(defaultGroups)
  })

  return (
    <div className='invites__container'>
      <GroupMainScreen
        param='invites'
        showAlert={showAlert}
        states={states}
        switchParamsOrdering={switchParamsOrdering}
        fetchMoreData={fetchMoreData}
      />
    </div>
  )
}
