import { useEffect, useState, ChangeEvent } from 'react'
import { useRecoilState, useRecoilValue, useSetRecoilState } from 'recoil'

import {
  CircularProgress,
  Container,
  MenuItem,
  Snackbar
} from '@material-ui/core'

import {
  tokenState,
  meInfoState,
  currentGroupState,
  authorsNamesState,
  ideasState,
  mobileOpenRightState,
  groupFiltersSelector,
  currentGroupIdFromLocationState
} from '../../../state'

import FormControl from '@material-ui/core/FormControl'
import Select from '@material-ui/core/Select'

import { useTranslation } from 'react-i18next'
import { CreateGroupInviteMembers } from '../../LeftSidebar/CreateGroup/CreateGroupInviteMembers/CreateGroupInviteMembers'
import {
  fetchIdeas,
  postInvites,
  fetchGroupRequests
} from '../../../functionsRequests'

import { GroupMainScreenDrawer } from '../GroupMainScreen/GroupMainScreenDrawer/GroupMainScreenDrawer'
import { CreateIdea } from '../CreateIdea/CreateIdea'
import { Search } from '../../Search/Search'
import { Alert } from '../../Alert/Alert'

import { GroupMainScreenIdea } from './GroupMainScreenIdea/GroupMainScreenIdea'

import '../GroupMainScreen/GroupMainScreen.scss'
import './GroupMainScreenIdeas.scss'

import InfiniteScroll from 'react-infinite-scroll-component'

import { GroupMainScreenInvite } from '../GroupMainScreen/GroupMainScreenInvite/GroupMainScreenInvite'
import { findAuthorForIdea } from '../../../functions'

export const GroupMainScreenIdeas = props => {
  const {
    param,
    states,
    switchParams,
    switchParamsOrdering,
    openId,
    members,
    setMembers
  } = props

  const token = useRecoilValue(tokenState)
  const [filterView, setFilterView] = useState('newest first')
  const { t } = useTranslation()

  const me = useRecoilValue(meInfoState)

  const [inviteMessage, setInviteMessage] = useState('')
  const [inviteMembers, setInviteMembers] = useState([])
  const [isInviteOpen, setIsInviteOpen] = useState(false)
  const [openSnackbar, setOpenSnackbar] = useState(false)
  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')
  const currentGroup = useRecoilValue(currentGroupState)
  const [allIdeas, setAllIdeas]: any = useRecoilState(ideasState)
  const [ideas, setIdeas]: any = useState([])
  const setAuthorsNames = useSetRecoilState(authorsNamesState)
  const [localParam, setLocalParam] = useState('')
  const [groupInvites, setGroupInvites]: any = useState([])
  const mobileOpenRight = useRecoilValue(mobileOpenRightState)

  const [isFetching, setIsFetching] = useState(true)
  const [urlParams, setUrlParams] = useState('')

  const groupFromLocation = useRecoilValue(currentGroupIdFromLocationState)
  const [groupId, setGroupId] = useState(currentGroup.id)

  const [filtersGroups, setFiltersGroups]: any = useRecoilState(
    groupFiltersSelector
  )

  const membersPlan = '\u221E'

  const findAssignee = idea => {
    if (allIdeas && allIdeas.users) {
      const names = allIdeas.users
      const author =
        names &&
        names.filter(author => {
          return author.id == idea.assignee
        })[0]
      return author && author.displayName ? author.displayName : ''
    }
  }

  const [
    openGroupMainScreenSettings,
    setOpenGroupMainScreenSettings
  ] = useState(false)

  const [state, setState] = useState(states)

  const handleChangeSwitch = event => {
    setState({ ...state, [event.target.name]: event.target.checked })
  }

  useEffect(() => {
    if (
      groupId !== currentGroup.id ||
      (ideas.length && ideas[0].groupId !== currentGroup.id) ||
        (!groupId && !currentGroup.id)
    ) {
      if (currentGroup.id) {
        setGroupId(currentGroup.id)
      } else {
        if (groupFromLocation) {
          setGroupId(groupFromLocation)
        }
      }
    }

    setLocalParam('')

    if (currentGroup.id !== groupId) {
      setState(filtersGroups[currentGroup.id] || states)
    }

    setAuthorsNames(allIdeas.users)
    if (
      groupId == '' &&
      (currentGroup.id !== '' || localStorage.getItem('currentGroupId'))
    ) {
      setGroupId(currentGroup.id || localStorage.getItem('currentGroupId'))
    }
  }, [currentGroup])

  useEffect(() => {
    let localUrlParams = ''

    if (param == 'ideas') {
      setFiltersGroups({ ...filtersGroups, [currentGroup.id]: state })
    } else {
      setFiltersGroups({ ...filtersGroups, [param]: state })
    }

    for (let urlParam in filtersGroups[
      param == 'ideas' ? currentGroup.id : param
    ]) {
      switch (urlParam) {
        case 'assigned':
          state[urlParam] ? (localUrlParams += `assignee=${me.sub}&`) : ''
          break
        case 'offered':
          state[urlParam] ? (localUrlParams += `author=${me.sub}&`) : ''
          break
        case 'voted':
          state[urlParam] ? (localUrlParams += `voted-by-me=true&`) : ''
          break
        case 'alreadyDone':
          state[urlParam] ? (localUrlParams += `implemented=true&`) : ''
          break
        case 'archived':
          state[urlParam] ? (localUrlParams += `archived=true&`) : ''
          break
        case 'ordering':
          state[urlParam] ? (localUrlParams += state[urlParam]) : ''
      }
    }
    if (currentGroup.id == groupId) {
      if (
        currentGroup.id &&
        param == 'ideas' &&
        (localUrlParams || urlParams)
      ) {
        setUrlParams(localUrlParams)
        ;(async () => {
          const ideasWithParams = await fetchIdeas(
            token,
            currentGroup.id,
            localUrlParams
          )
          if (
            ideasWithParams.ideas.length % 10 !== 0 ||
            ideasWithParams.ideas.length == 0
          ) {
            setIsFetching(false)
          } else {
            setIsFetching(true)
          }

          if ((await ideasWithParams) !== 'undefined') {
            setAllIdeas(ideasWithParams)
            setIdeas(ideasWithParams.ideas)
          }
        })()
      } else if (param == 'ideas') {
        if (allIdeas && allIdeas.ideas == undefined) {
          fetchIdeas(token, currentGroup.id, localUrlParams).then(res => {
            if (res !== 'undefined') {
              setAllIdeas(res)
              setIdeas(res.ideas)
            }
          })
        } else if (
          (allIdeas && allIdeas.ideas) ||
          (allIdeas && allIdeas.ideas.length == 0)
        ) {
            setIdeas(allIdeas.ideas)
        }
        if (allIdeas.ideas && allIdeas.ideas.length == 0) {
          setIsFetching(false)
        }
      }
    } else if (openId) {
      if (allIdeas && allIdeas.ideas) {
        setIdeas(allIdeas.ideas)
      }
    }
  }, [state, currentGroup])

  const handleClickSettings = () => {
    setOpenGroupMainScreenSettings(true)
  }

  const handleClose = () => {
    setOpenGroupMainScreenSettings(false)
  }

  const showAlert = (open, severity, message) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)
    setTimeout(() => {
      setOpenSnackbar(false)
    }, 1000)
  }

  const handleCloseSnackbar = (
    event?: React.SyntheticEvent,
    reason?: string
  ) => {
    if (reason === 'clickaway') {
      return
    }
  }

  const createInviteMembers = () => {
    ;(async () => {
      if (await postInvites(token, groupId, inviteMembers, inviteMessage)) {
        showAlert(true, 'success', t('Members invited successfully'))
      } else {
        showAlert(true, 'error', t('Something went wrong'))
      }
    })()
    setInviteMessage('')
    setInviteMembers([])
    setIsInviteOpen(false)
  }

  const handleInviteMembers = () => {
    setIsInviteOpen(true)
  }

  const handleManageJoinRequests = () => {
    if (!localParam) {
      setLocalParam('manage')

      document
        .getElementById('manageJoinRequest')
        .classList.add('LeftSideBar__link--active')
      ;(async () => {
        setGroupInvites(await fetchGroupRequests(token, groupId))
      })()
    } else {
      document
        .getElementById('manageJoinRequest')
        .classList.remove('LeftSideBar__link--active')
      setLocalParam(null)
    }
  }

  const handleChangeFilter = (e: ChangeEvent<{ value: unknown }>) => {
    setFilterView(e.target.value as string)

    const expr = e.target.value

    switch (expr) {
      case 'newest first':
        setState({ ...state, ['ordering']: 'ordering=CTIME_DESC&' })
        break
      case 'most voted first':
        setState({ ...state, ['ordering']: 'ordering=VOTES_DESC&' })
        break
      case 'most commented first':
        break
      case 'older first':
        setState({ ...state, ['ordering']: 'ordering=CTIME_ASC&' })
        break
    }
  }

  useEffect(() => {}, [isInviteOpen])

  const handleChangeSearch = e => {
    const searchValue = e.target.value

    if (searchValue.length >= 3) {
      ;(async () => {
        setIdeas(
          (await fetchIdeas(token, currentGroup.id, `&text=${searchValue}`))
            .ideas
        )
      })()
    } else {
      ;(async () => {
        setIdeas((await fetchIdeas(token, currentGroup.id, null)).ideas)
      })()
    }
  }

  const fetchMoreData = (
    isMore?: boolean,
    newIdeas?: any,
    localURLParams?: string
  ) => {
    if (
      (isFetching && ideas.length % 10 == 0 && ideas.length !== 0) ||
      isMore ||
      newIdeas
    ) {
      ;(async () => {
        const moreLength =
          isMore && newIdeas.length !== 0 ? 11 - (newIdeas.length % 10) : 10
        const curIdeas = isMore ? newIdeas : ideas
        const newFetchIdeas = await fetchIdeas(
          token,
          groupId,
          localURLParams ? localURLParams : urlParams,
          curIdeas.length,
          moreLength
        )

        if ((await newFetchIdeas) !== 'undefined') {
          setAllIdeas({
            ideas: allIdeas.ideas.concat((await newFetchIdeas).ideas),
            users: allIdeas.users.concat((await newFetchIdeas).users)
          })
          setIdeas(
            newIdeas
              ? newIdeas.concat((await newFetchIdeas).ideas)
              : ideas.concat((await newFetchIdeas).ideas)
          )

          if (
            newFetchIdeas.ideas.length % 10 !== 0 ||
            newFetchIdeas.ideas.length == 0
          ) {
            setIsFetching(false)
          }
        }

        if (!isMore && newIdeas) {
          setAllIdeas({
            ideas: newIdeas,
            users: allIdeas.users
          })
          setIdeas(newIdeas)
        }
      })()
    } else {
      setIsFetching(false)
    }
  }

  if (!groupId && param == 'ideas') {
    return (
      <div className='groupMainScreen'>
        <Container className='groupMainScreen__container'>
          <div className='row groupMainScreen__row'>
            <p>Group not found!</p>

            <div className='groupMainScreen__drawer groupMainScreen__drawer--left'>
              <div className='groupMainScreen__rightSightbar'>
                <div>
                  <div className='groupMainScreen__toolbar'></div>
                </div>
              </div>
            </div>
          </div>
        </Container>
      </div>
    )
  }
  return (
    <div className='groupMainScreen'>
      <Container className='groupMainScreen__container'>
        <div className='row groupMainScreen__row'>
          <Search
            placeholder={
              param == 'ideas' && !localParam ? 'Search Idea' : 'Request'
            }
            handleChangeSearch={handleChangeSearch}
          />

          <FormControl className='groupMainScreen__select'>
            <Select value={filterView} onChange={handleChangeFilter}>
              {switchParamsOrdering &&
                switchParamsOrdering.map(switchParam => (
                  <MenuItem key={switchParam.name} value={switchParam.name}>
                    {t(switchParam.name)}
                  </MenuItem>
                ))}
            </Select>
          </FormControl>
        </div>
        {param == 'ideas' &&
          !localParam &&
          !openGroupMainScreenSettings &&
          ideas &&
          allIdeas &&
          Array.isArray(ideas) && (
            <div id='groupMainScreenIdeas' className='groupMainScreenIdeas'>
              <InfiniteScroll
                dataLength={ideas.length}
                next={fetchMoreData}
                hasMore={ideas.length == 0 ? false : isFetching}
                loader={
                  <div className='groupMainScreenIdeas__progress'>
                    <CircularProgress />
                  </div>
                }
                scrollableTarget='groupMainScreenIdeas'
              >
                {ideas.map(idea => (
                  <GroupMainScreenIdea
                    key={idea.id}
                    idea={idea}
                    author={findAuthorForIdea(idea, allIdeas, me, 'idea')}
                    assignee={findAssignee(idea)}
                    openId={openId}
                    setIdeas={setIdeas}
                    ideas={ideas}
                    findAssignee={findAssignee}
                    members={members}
                    fetchMoreData={fetchMoreData}
                  />
                ))}
              </InfiniteScroll>
            </div>
          )}
        {param == 'ideas' && !localParam && (
          <CreateIdea
            groupId={groupId}
            param='CREATE'
            fetchMoreData={fetchMoreData}
            ideas={allIdeas}
          />
        )}

        {localParam && groupInvites && (
          <div className='groupMainScreen__invites'>
            {groupInvites.joinRequests &&
              groupInvites.joinRequests.map(invite => (
                <GroupMainScreenInvite
                  key={invite.id}
                  invite={invite}
                  invites={groupInvites}
                  setInvites={setGroupInvites}
                  localParam={localParam}
                  setMembers={setMembers}
                  showAlert={showAlert}
                />
              ))}
          </div>
        )}
      </Container>
      {!openId && (
        <div
          className={`groupMainScreen__drawerPaper groupMainScreen__drawer--right ${
            mobileOpenRight ? 'groupMainScreen__drawer--right-open' : ''
          } `}
        >
          <GroupMainScreenDrawer
            param={param}
            me={me}
            switchParams={switchParams}
            state={filtersGroups[currentGroup.id] || state}
            handleChangeSwitch={handleChangeSwitch}
            members={members}
            setMembers={setMembers}
            handleInviteMembers={handleInviteMembers}
            handleClickSettings={handleClickSettings}
            group={currentGroup}
            openGroupMainScreenSettings={openGroupMainScreenSettings}
            setOpenGroupMainScreenSettings={setOpenGroupMainScreenSettings}
            handleClose={handleClose}
            handleManageJoinRequests={handleManageJoinRequests}
          />
        </div>
      )}

      {isInviteOpen && (
        <CreateGroupInviteMembers
          handleCreateGroup={createInviteMembers}
          setNext={setIsInviteOpen}
          members={inviteMembers}
          setMembers={setInviteMembers}
          membersPlan={membersPlan}
          inviteMessage={inviteMessage}
          setInviteMessage={setInviteMessage}
          isNew={false}
          groupParams={currentGroup}
        />
      )}

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
