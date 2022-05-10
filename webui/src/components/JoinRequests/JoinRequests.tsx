import { useEffect } from 'react'
import { useSetRecoilState } from 'recoil'
import { GroupMainScreen } from '../Group/GroupMainScreen/GroupMainScreen'
import { currentGroupState } from './../../state'

import { switchParamsOrdering } from '../../functions'
import { defaultGroups } from '../../types/Groups'

import './JoinRequests.scss'

export const JoinRequests = props => {
  const { showAlert } = props

  const setGroup = useSetRecoilState(currentGroupState)

  useEffect(() => {
    setGroup(defaultGroups)
  }, [])

  const states = {
    ordering: ''
  }

  return (
    <div className='joinRequests__container'>
      <GroupMainScreen
        param='requests'
        states={states}
        switchParamsOrdering={switchParamsOrdering}
        showAlert={showAlert}
      />
    </div>
  )
}
