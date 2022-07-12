import React, { Suspense, useState, useEffect, lazy } from 'react'
import { useRecoilState, useRecoilValue } from 'recoil'
import {
  allUserGroupsState,
  infoMeAtomState,
  mobileOpenRightState,
  mobileOpenState
} from './state'
import { tokenState } from './state'
import { BrowserRouter as Router, Switch, Route } from 'react-router-dom'
import { getAllUrlParams } from './functions'
import { fetchAllGroups } from './functionsRequests'
import CircularProgress from '@material-ui/core/CircularProgress'
const MainScreen = lazy(() => import('./components/MainScreen/MainScreen'))
const Navbar = lazy(() => import('./components/Navbar/Navbar'))
const Group = lazy(() => import('./components/Group/Group'))
const AvailiableGroups = lazy(() => import('./components/AvailiableGroups/AvailiableGroups'))
const Invites = lazy(() => import('./components/Invites/Invites'))
const JoinRequests = lazy(() => import('./components/JoinRequests/JoinRequests'))
const JoinRequestsModal = lazy(() => import('./components/JoinRequests/JoinRequestsModal/JoinRequestsModal'))
import { ShowAlert } from './components/Alert/Alert'
const NotFound = lazy(() => import('./components/NotFound/NotFound'))

const localToken = null
//import { localToken } from '../localToken.js'

const LeftSidebar = React.lazy(() =>
  import('./components/LeftSidebar/LeftSidebar')
)

import './App.scss'

const App: React.FC = () => {
  const info: any = useRecoilValue(infoMeAtomState)
  const [mobileOpen, setMobileOpen] = useRecoilState(mobileOpenState)
  const [mobileOpenRight, setMobileOpenRight] = useRecoilState(
    mobileOpenRightState
  )

  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')
  const [openSnackbar, setOpenSnackbar] = useState(false)

  const [token, setToken] = useRecoilState(tokenState)
  const [isInfoValue, setIsInfoValue] = useState(false)
  const [isError, setIsError] = useState(false)

  const [groups, setGroups]: any = useRecoilState(allUserGroupsState)
  const [localGroups, setLocalGroups] = useState(groups)

  const [isFetching, setIsFetching] = useState(true)

  useEffect(() => {
    if (info && info.sub) {
      setIsInfoValue(true)
    }

    if (!info && token) {
      setIsError(true)
    }
  }, [info])

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen)
  }

  const handleDrawerToggleRight = () => {
    setMobileOpenRight(!mobileOpenRight)
  }

  const showAlert = (open, severity, message) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)
    setTimeout(() => setOpenSnackbar(false), 2000)
  }

  useEffect(() => {
    if (!token) {
      if (window.location.href == 'http://127.0.0.1:8080/' || window.location.href == 'http://localhost:8080/') {
        // Для локальной разработки нужен файл localToken.js в папке webui  с export const localToken = 'Токе любого пользователя'
        if (localToken) {
            setToken(localToken)
            localStorage.setItem('jwt', localToken)
        }
      }

      if (
        getAllUrlParams(window.location.href) &&
        getAllUrlParams(window.location.href).jwt
      ) {
        setToken(getAllUrlParams(window.location.href).jwt)
        localStorage.setItem('jwt', getAllUrlParams(window.location.href).jwt)
      }

      if (localStorage.getItem('jwt')) {
        setToken(localStorage.getItem('jwt'))
      }
    }
  }, [token])

  const fetchMoreData = (isMore?: boolean, changedGroups?, isAdd?: string) => {
    if (
      (isFetching &&
        localGroups.length % 10 == 0 &&
        localGroups.length !== 0) ||
      isMore
    ) {
      const moreLength = isMore ? 11 - (localGroups.length % 10) : 10
      const curGroups = isMore ? changedGroups : localGroups
      const last = isAdd ? moreLength - 2 : moreLength

      fetchAllGroups(token, info.sub, curGroups.length, last).then(res => {
        if (res !== 'undefined' && res !== undefined) {
          setGroups(curGroups.concat(res.groups))
          setLocalGroups(curGroups.concat(res.groups))

          if (
            (curGroups.concat(res.groups).length % 10 !== 0 && isMore) ||
            res.groups.length == 0
          ) {
            setIsFetching(false)
          }
        }
      })
    } else {
      setIsFetching(false)
    }
  }

  return (
    <div className={`app ${document.location.pathname == '/' ? 'app--full' : ''}`}>
      {!isInfoValue && isError && (
        <ShowAlert
          open={true}
          severity='error'
          message='Something went wrong! Please try login again!'
        />
      )}

      {!isInfoValue && !isError && localStorage.getItem('jwt') && (
        <ShowAlert
          open={true}
          severity='error'
          message='Something went wrong!'
        />
      )}
      <Router>
        <Switch>
          <Route exact path='/' component={() => <MainScreen />} />
          {isInfoValue && (
            <>
              <Navbar
                handleDrawerToggle={handleDrawerToggle}
                handleDrawerToggleRight={handleDrawerToggleRight}
                showAlert={showAlert}
              />
              <LeftSidebar
                mobileOpen={mobileOpen}
                handleDrawerToggle={handleDrawerToggle}
                showAlert={showAlert}
                fetchMoreData={fetchMoreData}
                isFetching={isFetching}
                localGroups={localGroups}
                setLocalGroups={setLocalGroups}
              />

              <Suspense
                fallback={
                  <div className='app__suspense'>
                    <CircularProgress />
                  </div>
                }
              >
                <Route exact path='/main' />

                <Route
                  exact
                  path='/group/:id'
                  component={() => <Group key={1} allGroups={groups} />}
                />
                <Route
                  exact
                  path='/ideas/:id'
                  component={() => (
                    <Group
                      openId={window.location.href.split('/')[4]}
                      allGroups={groups}
                    />
                  )}
                />
                <Route
                  exact
                  path='/availiable-groups/'
                  component={() => (
                    <AvailiableGroups
                      fetchMoreData={fetchMoreData}
                      showAlert={showAlert}
                    />
                  )}
                />
                <Route
                  exact
                  path='/join-requests'
                  component={() => <JoinRequests showAlert={showAlert} />}
                />
                <Route
                  exact
                  path='/invites/'
                  component={() => (
                    <Invites
                      fetchMoreData={fetchMoreData}
                      showAlert={showAlert}
                    />
                  )}
                />
                <Route
                  path='/groups/inviteLink=:id'
                  component={() => <JoinRequestsModal showAlert={showAlert} />}
                />
              </Suspense>
              {openSnackbar && (
                <ShowAlert
                  open={openSnackbar}
                  message={alertMessage}
                  severity={severity}
                />
              )}
            </>
          )}
          {!token && (
            <Route
              path='/groups/inviteLink=:id'
              component={() => <MainScreen />}
            />
          )}
          <Route path='*' component={NotFound} />
        </Switch>
      </Router>
    </div>
  )
}

export default App
