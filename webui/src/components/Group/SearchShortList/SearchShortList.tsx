import { Dispatch, SetStateAction, useEffect, useState } from 'react'
import { useRecoilValue } from 'recoil'

import { Snackbar } from '@material-ui/core'

import { currentGroupState, meInfoState, tokenState } from '../../../state'
import {
  fetchMembers,
  fetchGroups,
  fetchMembersSearch
} from '../../../functionsRequests'

import { GroupMainScreenMember } from '../GroupMainScreen/GroupMainScreenMember/GroupMainScreenMember'
import { Search } from '../../Search/Search'
import { Alert } from '../../Alert/Alert'

import { SearchShortListProps } from './SearchShortListInterfaces'
import { Groups } from '../../../types/Groups'

import { useTranslation } from 'react-i18next'

import './SearchShortList.scss'

export const SearchShortList = (props: SearchShortListProps) => {
  const {
    changedItem,
    setChangedItem,
    param,
    isAdmin,
    curMembers,
    setMembers
  } = props

  const [localMembers, setLocalMembers] = useState(curMembers)
  const group = useRecoilValue(currentGroupState)
  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')
  const [openSnackbar, setOpenSnackbar] = useState(false)
  const [groups, setGroups]: [
    Groups[] | [],
    Dispatch<SetStateAction<Groups[] | []>>
  ] = useState([])

  const token = useRecoilValue(tokenState)
  const me = useRecoilValue(meInfoState)
  const userId = me.sub

  const { t } = useTranslation()

  useEffect(() => {
    setLocalMembers(curMembers)
  }, [curMembers])

  const handleCloseSnackbar = (
    event?: React.SyntheticEvent,
    reason?: string
  ) => {
    if (reason === 'clickaway') {
      return
    }
  }

  const showAlert = (open, severity, message) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)
    setTimeout(() => setOpenSnackbar(false), 1000)
  }

  const handleChangeSearch = (event: React.ChangeEvent) => {
    const element = event.currentTarget as HTMLInputElement
    const searchValue = element.value

    if (searchValue.length > 2) {
      ;(async () => {
        if (param == 'change') {
          setLocalMembers(
            await fetchMembersSearch(token, group.id, searchValue)
          )
        } else if (param == 'groupMain') {
          setLocalMembers(
            await fetchMembersSearch(token, group.id, searchValue)
          )
        } else {
          setGroups(await fetchGroups(token, userId, searchValue))
        }
      })()
    } else if (searchValue.length == 0) {
      ;(async () => {
        if (param == 'change') {
          setLocalMembers(await fetchMembers(token, group.id))
        } else if (param == 'groupMain') {
          setLocalMembers(await fetchMembers(token, group.id))
        } else {
          setGroups(await fetchGroups(token, userId))
        }
      })()
    }
  }

  return (
    <div className='searchShortList'>
      <Search
        placeholder={
          param == 'change' || param == 'groupMain'
            ? t('Search member')
            : t('Search group')
        }
        handleChangeSearch={handleChangeSearch}
      />
      <div className='searchShortList__list'>
        {localMembers &&
          (param == 'change' || param == 'groupMain') &&
          localMembers.map((member, i) => (
            <GroupMainScreenMember
              key={member.userId}
              member={member}
              param='select'
              changedMember={changedItem}
              setChangedMember={setChangedItem}
              paramMain={param == 'groupMain' ? param : ''}
              showAlert={showAlert}
              isAdmin={isAdmin}
              members={localMembers}
              setLocalMembers={setLocalMembers}
              setMembers={setMembers}
            />
          ))}
        {groups &&
          param == 'move' &&
          groups.map(group => (
            <GroupMainScreenMember
              key={group.id}
              member={group}
              param='group'
              changedMember={changedItem}
              setChangedMember={setChangedItem}
              showAlert={showAlert}
            />
          ))}
      </div>
      <Snackbar
        open={openSnackbar}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
      >
        <Alert onClose={handleCloseSnackbar} severity={severity}>
          {alertMessage}
        </Alert>
      </Snackbar>
    </div>
  )
}
