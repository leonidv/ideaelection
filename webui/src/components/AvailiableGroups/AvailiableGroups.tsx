import { useTranslation } from 'react-i18next'
import GroupMainScreen from '../Group/GroupMainScreen/GroupMainScreen'

import { switchParamsOrderingGroups } from '../../functions'

import './AvailiableGroups.scss'
import { currentGroupState } from '../../state'
import { useSetRecoilState } from 'recoil'
import { useEffect } from 'react'
import { defaultGroups } from '../../types/Groups'

const states = {
  onlyPublic: false,
  notIncludeWithJoinRequest: false,
  notIncludeWithInvites: false,
  ordering: ''
}

const AvailiableGroups = props => {
  const { fetchMoreData, showAlert } = props
  const { t } = useTranslation()
  const setGroup = useSetRecoilState(currentGroupState)

  useEffect(() => {
    setGroup(defaultGroups)
  })

  const switchParams = [
    {
      title: t('Only public'),
      name: 'onlyPublic'
    },
    {
      title: t('With my join requests'),
      name: 'notIncludeWithJoinRequest'
    },
    {
      title: t('Which are invited me'),
      name: 'notIncludeWithInvites'
    }
  ]

  return (
    <div className='availiableGroups__container'>
      <GroupMainScreen
        param='group'
        states={states}
        switchParams={switchParams}
        switchParamsOrdering={switchParamsOrderingGroups}
        fetchMoreData={fetchMoreData}
        showAlert={showAlert}
      />
    </div>
  )
}

export default AvailiableGroups;
