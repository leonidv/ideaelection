import React, { Suspense, useState, useEffect } from 'react'
import CircularProgress from '@material-ui/core/CircularProgress'
import { MainScreen } from './components/MainScreen/MainScreen'
import Navbar from './components/Navbar/Navbar'
import { Group } from './components/Group/Group'
import { AvailiableGroups } from './components/AvailiableGroups/AvailiableGroups'
import { Invites } from './components/Invites/Invites'
import { JoinRequests } from './components/JoinRequests/JoinRequests'
import { useRecoilState, useRecoilValue } from 'recoil'
import {
  allUserGroupsState,
  infoMeAtomState,
  mobileOpenRightState,
  mobileOpenState
} from './state'
import { tokenState } from './state'
import { BrowserRouter as Router, Switch, Route } from 'react-router-dom'
import { ShowAlert } from './components/Alert/Alert'
import { NotFound } from './components/NotFound'

import { getAllUrlParams } from './functions'

import './App.scss'
import { JoinRequestsModal } from './components/JoinRequests/JoinRequestsModal/JoinRequestsModal'
import { fetchAllGroups } from './functionsRequests'

const LeftSidebar = React.lazy(() =>
  import('./components/LeftSidebar/LeftSidebar')
)

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
      console.log(token)
      if (window.location.href == 'http://127.0.0.1:8080/' || window.location.href == 'http://localhost:8080/') {
        const localToken =
          'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMDMwOTQxMzc0NDQwODc5MDAxMDlAZ29vZ2xlIiwiZGlzcGxheU5hbWUiOiJUYXR5bmEgUGV0cnVraGluYSIsInN1YnNjcmlwdGlvblBsYW4iOiJGUkVFIiwicm9sZXMiOiJST0xFX1VTRVIiLCJpc3MiOiJzYWVkaSIsImF2YXRhciI6Imh0dHBzOlwvXC9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tXC9hXC9BQVRYQUp4SXU0bUF2YTRvSnhRSndIY0VXTGMzM0x4ZGF4V2h1NVBKWWc2Zj1zOTYtYyIsImV4cCI6MzMxNzMyMjIxMTQsImlhdCI6MTYzNzIyMjExNCwiZW1haWwiOiJ2bGFkaXNsYXZhYXRyb2ZpbW92YUBnbWFpbC5jb20ifQ.YS09jxUGB_0AbnAlZx60fK9pn9_WVPIf_lLcEouKaV_CrlK308SEV5WvVQ6di1UGIOza3u3FBggxHc1hJmijjWr71Bp19v6bhHhJ1P8ba7t9AYm0hCw20_wViRA-Z0gVnjKVtyQvTgxt7aLM-NY4Brpt7VUrR2OExjWjkSjuUho-eL58LPbN0P1P6Sg_Gf13kwsV-pjCsesheZqlZIV4vh0hj3LIFLVJpw0Onsbjr8Lo657wsX-CFhrvC2ppHK4u_779wXJrqsFP1LXmmlZPT2x-KhoqsigOjOr0f-UuNAdDRgYv-yUM0MxASjzrF5WXYhd5avKQnCy1lMqb8ZOduQ'
        //const localToken = 'eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMDk3MDM1NTEyNDgzNTY5NzYzMjRAZ29vZ2xlIiwiZGlzcGxheU5hbWUiOiLQnNCw0YDQuNGPINCf0LXRgtGA0YPRhdC40L3QsCIsInJvbGVzIjoiUk9MRV9VU0VSIiwiaXNzIjoic2FlZGkiLCJhdmF0YXIiOiJodHRwczpcL1wvbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbVwvYVwvQUFUWEFKeVpLTjE3OE9SYS1PRUQzR1FlalZhUWpsNVJBSjloWjlrV0VnNlY9czk2LWMiLCJleHAiOjMzMTcwMTI2NzAxLCJpYXQiOjE2MzQxMjY3MDEsImVtYWlsIjoicHRyaHNtQG1haWwucnUifQ.NIc3HR1GzrIHCuR0Ldo9AMFFFkNptwekUiQd--9JHtIUBFPW_x2Rxcf5ZmHfAVp8tYV2m5AFo6D64CBycm9F48D5t_nilm364JY5Tw3Z-873viNv4tw_uWEZW6rTEXcAVjWd0CaLvHjPNioHOF7OtZF__SJbKZXj792WH8UBbnOu6Z6PWFk5RiwTXMyYAu5yDR8Cl-D0YMm2LuiLWhuPLpJ12CjHX3yef7jwiMH7x6wto6kOoB51IG690doz91hHe32eiE64yju1v-mQY2puyi-B_q9t0rJr3Xz5tpFL4YZXZ9UNU7d1dhCIKcPIc8c7dMgOz4JorcM5WOYfPWw1Xg'
        setToken(localToken)
        localStorage.setItem('jwt', localToken)
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
    <div className='app'>
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
