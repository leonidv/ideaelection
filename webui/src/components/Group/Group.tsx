import {
  useRecoilState,
  useRecoilValue,
  useResetRecoilState,
  useSetRecoilState
} from 'recoil'
import {
  tokenState,
  currentGroupState,
  ideasState,
  meInfoState,
  membersState
} from '../../state'
import { fetchAllGroups, fetchGroup } from '../../functionsRequests'

import { GroupMainScreenIdeas } from './GroupMainScreenIdeas/GroupMainScreenIdeas'

import './Group.scss'

import { useTranslation } from 'react-i18next'
import { useEffect, useState } from 'react'
import { Groups } from '../../types/Groups'

import { GroupProp } from './GroupInterfaces'

const states = {
  assigned: false,
  offered: false,
  voted: false,
  alreadyDone: false,
  archived: false,
  ordering: null
}

const Group = (props: GroupProp) => {
  const { openId, allGroups } = props
  const me = useRecoilValue(meInfoState)
  const [group, setGroup] = useRecoilState<Groups>(currentGroupState)
  const setIdeas = useSetRecoilState(ideasState)
  const token = useRecoilValue(tokenState)
  const [groupId, setGroupId] = useState(
    window.location.href.split('/')[3] == 'group'
      ? window.location.href.split('/')[4]
      : ''
  )
  const [members, setMembers] = useRecoilState(membersState)
  const resetMembers = useResetRecoilState(membersState)

  const { t } = useTranslation()

  useEffect(() => {
    if (
      window.location.href.split('/')[3] == 'group' &&
      window.location.href.split('/')[4] !== group.id
    ) {
      if (
        window.location.href.split('/')[4] !== groupId ||
        (!group.id && window.location.href.split('/')[4] == groupId)
      ) {
        setGroupId(window.location.href.split('/')[4])
        if (me) {
          ;(async () => {
            const newGroup = await fetchGroup(
              token,
              window.location.href.split('/')[4]
            )

            if ((await newGroup) !== 'undefined') {
              setGroup(await newGroup)
              resetMembers()
            }
          })()
          if (window.location.href.split('/')[3] !== 'ideas') {
            setIdeas([])
          }
        }
      } else if (allGroups && allGroups.length) {
        allGroups.map(group => {
          group.id == groupId ? setGroup(group) : ''
        })
        if (!group.id && window.location.href.split('/')[3] !== 'ideas') {
          if (allGroups.length % 10 == 0 && allGroups.length !== 0) {
            ;(async () => {
              const newGroups = await fetchAllGroups(
                token,
                me.sub,
                allGroups.length,
                10
              )

              if ((await newGroups) !== 'undefined') {
                newGroups.map(group => {
                  group.id == groupId ? setGroup(group) : ''
                })
              }
            })()
          }
        }
      }
    }
  }, [group, window.location.href.split('/')[4]])

  const switchParams = [
    {
      title: t('Assigned to me'),
      name: 'assigned'
    },
    {
      title: t('Offered by me'),
      name: 'offered'
    },
    {
      title: t('I voted'),
      name: 'voted'
    },
    {
      title: t('Already done'),
      name: 'alreadyDone'
    },
    {
      title: t('Archived'),
      name: 'archived'
    }
  ]

  const switchParamsOrdering = [
    {
      name: 'newest first'
    },
    {
      name: 'most voted first'
    },
    {
      name: 'most commented first'
    },
    {
      name: 'older first'
    }
  ]
  if (
    (window.location.href.split('/')[3] == 'group' &&
      window.location.href.split('/')[4] == group.id) ||
    window.location.href.split('/')[3] == 'ideas'
  ) {
    return (
      <div className='group'>
        <GroupMainScreenIdeas
          param='ideas'
          switchParams={switchParams}
          switchParamsOrdering={switchParamsOrdering}
          states={states}
          openId={openId}
          members={members}
          setMembers={setMembers}
        />
      </div>
    )
  } else return <></>
}

export default Group;
