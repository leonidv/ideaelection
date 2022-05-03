import { useEffect, useState, ChangeEvent } from 'react'
import { useRecoilState, useRecoilValue } from 'recoil'

import { Container, MenuItem } from '@material-ui/core'

import {
  tokenState,
  invitesState,
  meInfoState,
  availablesGroupsState,
  mobileOpenRightState,
  groupFiltersSelector,
  joinRequestsState
} from '../../../state'

import FormControl from '@material-ui/core/FormControl'
import Select from '@material-ui/core/Select'
import { CircularProgress } from '@material-ui/core'
import InfiniteScroll from 'react-infinite-scroll-component'

import { useTranslation } from 'react-i18next'
import { GroupMainScreenInvite } from './GroupMainScreenInvite/GroupMainScreenInvite'
import {
  fetchInvites,
  fetchAvailibaleGroups,
  fetchRequests
} from '../../../functionsRequests'

import { GroupMainScreenDrawer } from './GroupMainScreenDrawer/GroupMainScreenDrawer'
import { Search } from '../../Search/Search'

import { GroupMainScreenProps } from './GroupMainScreenInterfaces'

import './GroupMainScreen.scss'
import { useLocation } from 'react-router'
import { ShowAlert } from '../../Alert/Alert'
import { getOrdering } from '../../../functions'

export const GroupMainScreen: React.FC<GroupMainScreenProps> = (
  props: GroupMainScreenProps
) => {
  const {
    param,
    states,
    switchParams,
    switchParamsOrdering,
    fetchMoreData
  } = props

  const location = useLocation()

  const token = useRecoilValue(tokenState)
  const [filterView, setFilterView] = useState('Newest first')
  const { t } = useTranslation()
  const me = useRecoilValue(meInfoState)

  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')
  const [openSnackbar, setOpenSnackbar] = useState(false)

  const mobileOpenRight = useRecoilValue(mobileOpenRightState)

  const [availables, setAvailables] =
    param == 'group' ? useRecoilState(availablesGroupsState) : []

  const [requests, setRequests]: any =
    param == 'requests' ? useRecoilState(joinRequestsState) : []
  const [urlParams, setUrlParams] = useState('')

  const [filtersGroups, setFiltersGroups]: any = useRecoilState(
    groupFiltersSelector
  )

  const [locationKey, setLocationKey] = useState('')

  const [state, setState] = useState(states)

  const showAlert = (open, severity, message) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)
    setTimeout(() => setOpenSnackbar(false), 2000)
  }

  const handleChangeSwitch = event => {
    changeURLParamsSwitch(event)
    setState({ ...state, [event.target.name]: event.target.checked })
  }

  const [isFetching, setIsFetching] = useState(true)

  const initGroupMainScreen = (arr, setArr, req) => {
    if ((!arr.groups && !arr.groups.length) || locationKey !== location.key) {
      req.then(res => {
        if (res && res !== 'undefined') {
          setArr(res)
        }
      })
      setLocationKey(location.key)
    }
  }

  useEffect(() => {
    if (param == 'group') {
      initGroupMainScreen(
        availables,
        setAvailables,
        fetchAvailibaleGroups(token, urlParams)
      )
    } else if (param == 'requests') {
      initGroupMainScreen(requests, setRequests, fetchRequests(token, me.sub))
    }
  }, [location])

  const [invites, setInvites]: any =
    param == 'invites' ? useRecoilState(invitesState) : []

  const changeURLParamsSwitch = event => {
    if (event && event.target) {
      const filterName = !event.target.name ? 'ordering' : event.target.name
      const value =
        filterName == 'ordering'
          ? getOrdering(event.target.value)
          : event.target.checked
      const localFilters = { ...state, [filterName]: value }
      let localURLParams = ''

      for (let urlParam in localFilters) {
        switch (urlParam) {
          case 'notIncludeWithInvites':
            localFilters[urlParam]
              ? (localURLParams += `notIncludeWithInvites=true&`)
              : ''
            break
          case 'notIncludeWithJoinRequest':
            localFilters[urlParam]
              ? (localURLParams += `notIncludeWithJoinRequest=true&`)
              : ''
            break
          case 'onlyPublic':
            localFilters[urlParam] ? (localURLParams += `onlyPublic=true&`) : ''
            break
          case 'ordering':
            localFilters[urlParam]
              ? (localURLParams += localFilters[urlParam])
              : ''
        }
      }

      setUrlParams(localURLParams)
      setIsFetching(true)

      if (param == 'group') {
        const availablesObj = {
          groups: [],
          invites: [],
          joinRequests: []
        }
        setAvailables(availablesObj)
        fetchMoreDataAvailables(true, availablesObj, localURLParams)
      } else if (param == 'invites') {
        const invitesObj = {
          groups: [],
          invites: [],
          users: []
        }
        setInvites(invitesObj)
        fetchMoreDataInvites(true, invitesObj, localURLParams)
      } else if (param == 'requests') {
        const requestsObj = {
          groups: [],
          joinRequests: [],
          users: []
        }
        setRequests(requestsObj)
        fetchMoreDataInvites(true, requestsObj, localURLParams)
      }
    }
  }

  useEffect(() => {
    setFiltersGroups({ ...filtersGroups, [param]: state })
  }, [state])

  const handleChangeSearch = e => {
    const searchValue = e.target.value

    if (searchValue.length >= 3) {
      if (param == 'group') {
        fetchAvailibaleGroups(token, `${urlParams}name=${searchValue}&`).then(
          res => {
            if (res !== undefined && res !== 'undefined') {
              setAvailables(res)
            }
          }
        )
      }

      if (param == 'invites') {
        fetchInvites(token, me.sub, `&name=${searchValue}`).then(res => {
          if (res !== 'undefined' && res !== undefined) {
            setInvites(res)
          }
        })
      }

      if (param == 'requests') {
        const newRequests = fetchRequests(token, me.sub, `&name=${searchValue}`)
        newRequests.then(res => {
          if (res !== 'undefined') {
            setRequests(res)
          }
        })
      }
    } else if (searchValue.length == 0) {
      if (param == 'group') {
        fetchAvailibaleGroups(token, urlParams).then(res => {
          if (res !== undefined && res !== 'undefined') {
            setAvailables(res)
          }
        })
      }

      if (param == 'invites') {
        fetchInvites(token, me.sub).then(res => {
          if (res !== 'undefined' && res !== undefined) {
            setInvites(res)
          }
        })
      }

      if (param == 'requests') {
        const newRequests = fetchRequests(token, me.sub)
        newRequests.then(res => {
          if (res !== 'undefined') {
            setRequests(res)
          }
        })
      }

      setIsFetching(true)
    }
  }

  const handleChangeFilter = (e: ChangeEvent<{ value: unknown }>) => {
    setFilterView(e.target.value as string)

    changeURLParamsSwitch(e)

    const expr = e.target.value

    switch (expr) {
      case t('Newest first'):
        setState({ ...state, ['ordering']: 'ordering=CTIME_DESC&' })
        break
      case t('Older first'):
        setState({ ...state, ['ordering']: 'ordering=CTIME_ASC&' })
        break
      case t('Title A-Z'):
        setState({ ...state, ['ordering']: 'ordering=NAME_ASC&' })
        break
      case t('Title Z-A'):
        setState({ ...state, ['ordering']: 'ordering=NAME_DESC&' })
        break
    }
  }

  const fetchMoreDataInvites = (
    isMore?: boolean,
    changedInvites?: any,
    localURLParams?: string
  ) => {
    let localInvites = invites;
    let fetchLocalInvites = fetchInvites;

    if (param == 'requests') {
      localInvites = requests;
      fetchLocalInvites = fetchRequests
    } 

    if (
      (isFetching &&
        Object.keys(localInvites) &&
        localInvites.groups &&
        localInvites.groups.length % 10 == 0 &&
        localInvites.groups.length !== 0) ||
      isMore
    ) {
      const moreLength = isMore ? 11 - (localInvites.groups.length % 10) : 10
      const curInvites = isMore ? changedInvites : localInvites

      fetchLocalInvites(
        token,
        me.sub,
        localURLParams ? localURLParams : urlParams,
        curInvites.groups.length,
        moreLength
      ).then(res => {
        if (res !== 'undefined') {
          const invitesObj:any = {
            groups: curInvites.groups.concat(res.groups),
            users: curInvites.users.concat(res.users)
          }
          if (param == 'invites') {
            invitesObj.invites = curInvites.invites.concat(res.invites)
            setInvites(invitesObj)
          } else if (param == 'requests') {
            invitesObj.joinRequests = curInvites.joinRequests.concat(res.joinRequests)
            setRequests(invitesObj)
          }

          if (res.groups.length % 10 !== 0 || res.groups.length == 0) {
            setIsFetching(false)
          }
        }
      })
    } else {
      setIsFetching(false)
    }
  }

  const fetchMoreDataAvailables = (
    isMore?: boolean,
    newInvites?: any,
    localURLParams?: string
  ) => {
    if (
      (isFetching &&
        Object.keys(availables) &&
        availables.groups.length % 10 == 0 &&
        availables.groups.length !== 0) ||
      isMore
    ) {
      ;(async () => {
        const moreLength =
          isMore && newInvites.groups.length !== 0
            ? 11 - (availables.groups.length % 10)
            : 10
        const curInvites = isMore ? newInvites : availables
        const newAvailables = await fetchAvailibaleGroups(
          token,
          localURLParams ? localURLParams : urlParams,
          curInvites.groups.length,
          moreLength
        )

        if ((await newAvailables) !== 'undefined') {
          const availablesObj = {
            groups: curInvites.groups.concat((await newAvailables).groups),
            invites: curInvites.invites.concat((await newAvailables).invites),
            joinRequests: curInvites.joinRequests.concat(
              (await newAvailables).joinRequests
            )
          }
          setAvailables(availablesObj)

          if (
            newAvailables.groups.length % 10 !== 0 ||
            newAvailables.groups.length == 0
          ) {
            setIsFetching(false)
          }
        }
      })()
    } else {
      setIsFetching(false)
    }
  }

  useEffect(() => {}, [availables, invites, requests, param])

  return (
    <div className='groupMainScreen'>
      <Container className='groupMainScreen__container'>
        <div className='row groupMainScreen__row'>
          <Search
            placeholder={t('Search groups')}
            handleChangeSearch={handleChangeSearch}
          />

          <FormControl className='groupMainScreen__select'>
            <Select
              labelId='demo-simple-select-label'
              id='demo-simple-select'
              value={filterView}
              onChange={handleChangeFilter}
            >
              <MenuItem value='Select ordering' disabled>
                {t('Select ordering')}
              </MenuItem>
              {switchParamsOrdering &&
                switchParamsOrdering.map(switchParam => (
                  <MenuItem key={switchParam.name} value={switchParam.name}>
                    {t(switchParam.name)}
                  </MenuItem>
                ))}
            </Select>
          </FormControl>
        </div>

        {param == 'invites' && invites && (
          <div id='invitesList' className='groupMainScreen__invites'>
            {invites.groups && (
              <InfiniteScroll
                dataLength={invites.groups.length}
                next={fetchMoreDataInvites}
                hasMore={invites.groups.length == 0 ? false : isFetching}
                loader={
                  <div className='groupMainScreen__progress'>
                    <CircularProgress />
                  </div>
                }
                scrollableTarget='invitesList'
              >
                {invites.groups.map(invite => (
                  <GroupMainScreenInvite
                    key={invite.id}
                    invite={invite}
                    invites={invites}
                    setInvites={setInvites}
                    fetchMoreDataInvites={fetchMoreDataInvites}
                    fetchMoreData={fetchMoreData}
                    showAlert={showAlert}
                  />
                ))}
              </InfiniteScroll>
            )}
          </div>
        )}

        {param == 'requests' && requests && (
          <div id='requestsList' className='groupMainScreen__invites'>
            {requests.groups && (
              <InfiniteScroll
                dataLength={requests.groups.length}
                next={fetchMoreDataInvites}
                hasMore={requests.groups.length == 0 ? false : isFetching}
                loader={
                  <div className='groupMainScreen__progress'>
                    <CircularProgress />
                  </div>
                }
                scrollableTarget='requestsList'
              >
                {requests.groups.map(request => (
                  <GroupMainScreenInvite
                    key={request.id}
                    invite={request}
                    invites={requests}
                    setInvites={setRequests}
                    param={param}
                    showAlert={showAlert}
                    fetchMoreDataInvites={fetchMoreDataInvites}
                  />
                ))}
              </InfiniteScroll>
            )}
          </div>
        )}
        {param == 'group' && availables && (
          <div id='availablesList' className='groupMainScreen__available'>
            {Object.keys(availables) && (
              <InfiniteScroll
                dataLength={availables.groups.length}
                next={fetchMoreDataAvailables}
                hasMore={availables.groups.length == 0 ? false : isFetching}
                loader={
                  <div className='groupMainScreen__progress'>
                    <CircularProgress />
                  </div>
                }
                scrollableTarget='availablesList'
              >
                {availables.groups.map(available => (
                  <GroupMainScreenInvite
                    key={available.id}
                    invite={available}
                    invites={availables}
                    setInvites={setAvailables}
                    param='available'
                    showAlert={showAlert}
                    fetchMoreDataInvites={fetchMoreDataAvailables}
                    fetchMoreData={fetchMoreData}
                  />
                ))}
              </InfiniteScroll>
            )}
          </div>
        )}
      </Container>

      <div
        className={`groupMainScreen__drawerPaper groupMainScreen__drawer--right ${
          mobileOpenRight ? 'groupMainScreen__drawer--right-open' : ''
        } `}
      >
        <GroupMainScreenDrawer
          param={param}
          switchParams={switchParams}
          state={filtersGroups[param] || state}
          handleChangeSwitch={handleChangeSwitch}
        />
      </div>
      {openSnackbar && (
        <ShowAlert
          open={openSnackbar}
          message={alertMessage}
          severity={severity}
        />
      )}
    </div>
  )
}
