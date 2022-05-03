import { useState, useEffect, MouseEvent, useRef } from 'react'
import { NavLink, useHistory } from 'react-router-dom'
import {
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ClickAwayListener,
  Grow,
  Paper,
  Popper,
  IconButton,
  MenuList,
  MenuItem,
  CircularProgress
} from '@material-ui/core'
import {
  MoreVertOutlined as MoreVertOutlinedIcon,
  FolderOutlined as FolderOutlinedIcon,
  ThumbUp as ThumbUpIcon,
  CreateNewFolderOutlined as CreateNewFolderOutlinedIcon,
  PersonAddOutlined as PersonAddOutlinedIcon,
  Close as CloseIcon
} from '@material-ui/icons'

import { useRecoilValue, useRecoilState } from 'recoil'
import {
  allUserGroupsState,
  infoMeAtomState,
  mobileOpenState,
  createGroupOpenState,
  currentGroupState
} from './../../state'

import { Groups } from './../../types/Groups'
import { tokenState } from './../../state'
import { Me } from '../../types/Me'
import { LeftSideBarProp } from './LeftSidebarInterfaces'

import { useTranslation } from 'react-i18next'

import { CreateGroup } from './CreateGroup/CreateGroup'
import InfiniteScroll from 'react-infinite-scroll-component'

import { leaveGroup } from '../../functionsRequests'
import { useWindowSize, whiteImg } from '../../functions'

import './LeftSideBar.scss'

const LeftSidebar: React.FC<LeftSideBarProp> = (props: LeftSideBarProp) => {
  const {
    handleDrawerToggle,
    showAlert,
    fetchMoreData,
    isFetching,
    localGroups,
    setLocalGroups
  } = props

  const [copyGroup, setCopyGroup]: any = useState([])
  const [openId, setOpenId]: any = useState(false)

  const token = useRecoilValue(tokenState)
  const info: Me = useRecoilValue(infoMeAtomState)
  const group = useRecoilValue(currentGroupState)
  const [groups, setGroups]: any = useRecoilState(allUserGroupsState)

  const [mobileOpen, setMobileOpen] = useRecoilState(mobileOpenState)

  const createGroupOpen = useRecoilValue(createGroupOpenState)

  let ref = useRef(null)
  const size = useWindowSize()

  const { t } = useTranslation()

  const userId = info.sub || ''

  const history = useHistory()

  const anchorRef = useRef(null)

  const bottomLinks = [
    {
      text: t('Available groups'),
      icon: <FolderOutlinedIcon />,
      path: '/availiable-groups/'
    },
    {
      text: t('Invites to groups'),
      icon: <CreateNewFolderOutlinedIcon />,
      path: '/invites/'
    },
    {
      text: t('My join requests to groups'),
      icon: <PersonAddOutlinedIcon />,
      path: '/join-requests/'
    }
  ]

  const options = [t('Leave group'), t('Copy group ...')]

  useEffect(() => {}, [isFetching])

  useEffect(() => {
    if (document.querySelector('#groupsList').clientHeight > 550) {
      fetchMoreData()
    }
  }, [])

  useEffect(() => {}, [group])

  useEffect(() => {
    setLocalGroups(groups)
  }, [groups])

  useEffect(() => {
    if (size.width <= 1300) {
      document.addEventListener('click', handleClickOutside, true)
      return () => {
        document.removeEventListener('click', handleClickOutside, true)
      }
    }
  })

  const fetchLeaveGroup = async (group: Groups) => {
    const groupId = group.id
    if (userId && groupId) {
      leaveGroup(token, groupId, userId).then(res => {
        if (res !== undefined && res !== 'undefined') {
          setGroups(groups.filter(group => group.id !== groupId))
          fetchMoreData(
            true,
            groups.filter(group => group.id !== groupId)
          )
          history.push('/main')
          showAlert(true, 'success', t('Left the group'))
        } else {
          showAlert(true, 'error', t('Cant leave the group'))
        }
      })
    } else {
      showAlert(true, 'error', t('Something went wrong'))
    }
  }

  const addHeight = i => {
    const groupsList: HTMLElement = document.querySelector(
      '#groupsList .infinite-scroll-component'
    )
    if (i == localGroups.length - 1) {
      if (groupsList) {
        groupsList.style.paddingBottom = '78px'
      }
    }
  }

  const removeHeight = () => {
    const groupsList: HTMLElement = document.querySelector(
      '#groupsList .infinite-scroll-component'
    )
    if (groupsList) {
      groupsList.style.paddingBottom = '0px'
    }
  }

  const handleOptionGroup = (e, group) => {
    e.preventDefault()
    const groupsList: HTMLElement = document.querySelector(
      '#groupsList .infinite-scroll-component'
    )
    if (e.target.innerText == t('Leave group')) {
      fetchLeaveGroup(group)
    } else if (e.target.innerText == t('Copy group ...')) {
      setCopyGroup(group)
    }

    if (groupsList) {
      groupsList.style.paddingBottom = '0'
    }

    setOpenId(false)
  }

  const handleToggle = (e, groupI) => {
    let id = null
    let el = e.target

    while (!id) {
      id = el.id
      el = el.parentElement
    }

    if (!!openId == false) {
      addHeight(groupI)
      setOpenId(id)
    } else {
      setOpenId(false)
      removeHeight()
    }
  }

  const handleClose = (event?: MouseEvent<Document, globalThis.MouseEvent>) => {
    if (anchorRef.current && anchorRef.current.contains(event.target)) {
      return
    }

    setOpenId(false)
  }

  const handleListKeyDown = event => {
    if (event.key === 'Tab') {
      event.preventDefault()
      setOpenId(false)
    }
  }

  const handleClickOutside = event => {
    if (
      mobileOpen &&
      ref.current &&
      !ref.current.contains(event.target) &&
      !createGroupOpen
    ) {
      setMobileOpen(!mobileOpen)
    }
  }

  const handleClickBottomNav = () => {
    size.width <= 1300 ? setMobileOpen(!mobileOpen) : ''
    if (document.querySelector('.LeftSideBar__link--active')) {
      document
        .querySelector('.LeftSideBar__link--active')
        .classList.remove('LeftSideBar__link--active')
    }
  }

  return (
    <nav
      className={`LeftSideBar__drawer ${
        mobileOpen ? 'LeftSideBar__drawer--open' : ''
      }`}
      aria-label='mailbox folders'
    >
      <div className='LeftSideBar__drawerPaper'>
        <div>
          {size.width < 1300 && (
            <div className='LeftSideBar__toolbar LeftSideBar__toolbar--mobile'>
              <IconButton
                color='inherit'
                className={
                  window.innerWidth > 500 ? 'LeftSidebar__Display' : ''
                }
                aria-label='open drawer'
                edge='start'
                onClick={handleDrawerToggle}
              >
                <CloseIcon />
              </IconButton>
            </div>
          )}
          <div className='LeftSideBar__toolbar'>
            <div className='LeftSideBar__toolbarTopLeft'>
              <ThumbUpIcon />
              <h1 className='LeftSideBar__title'>Saedi</h1>
            </div>
            <CreateGroup
              copyGroup={copyGroup}
              setCopyGroup={setCopyGroup}
              fetchMoreData={fetchMoreData}
            />
          </div>
          <Divider />
          <List id='groupsList' className='LeftSideBar__list'>
            {groups && localGroups && (
              <InfiniteScroll
                dataLength={localGroups.length}
                next={fetchMoreData}
                hasMore={localGroups.length == 0 ? false : isFetching}
                loader={
                  <div className='LeftSideBar__progress'>
                    <CircularProgress />
                  </div>
                }
                scrollableTarget='groupsList'
              >
                {localGroups.map(
                  (group, groupI) =>
                    group.id && (
                      <div
                        key={group.id}
                        className={`LeftSideBar__link ${
                          group.id == window.location.href.split('/')[4]
                            ? 'LeftSideBar__link--active'
                            : ''
                        }`}
                      >
                        <NavLink
                          key={group.id}
                          className='LeftSideBar__item'
                          onClick={() => {
                            size.width <= 1300 ? setMobileOpen(!mobileOpen) : ''
                          }}
                          to={{
                            pathname: `/group/${group.id}`,
                            state: { group: group }
                          }}
                        >
                          <ListItem className='row leftSideBar__list-item'>
                            <>
                              <img
                                className='LeftSideBar__avatar avatar'
                                alt={group.name}
                                src={group.logo}
                                onError={({ currentTarget }) => {
                                  currentTarget.onerror = null
                                  currentTarget.src = whiteImg
                                }}
                              />
                              <ListItemText primary={group.name} />
                            </>
                          </ListItem>

                          <Popper
                            className='LeftSideBar__openMenu'
                            open={openId == `more${group.id}` ? true : false}
                            anchorEl={anchorRef.current}
                            role={undefined}
                            transition
                            disablePortal={true}
                          >
                            {({ TransitionProps, placement }) => (
                              <Grow
                                {...TransitionProps}
                                style={{
                                  transformOrigin:
                                    placement === 'bottom'
                                      ? 'center top'
                                      : 'center bottom'
                                }}
                              >
                                <Paper>
                                  <ClickAwayListener onClickAway={handleClose}>
                                    <MenuList
                                      autoFocusItem={
                                        openId == `more${group.id}`
                                          ? true
                                          : false
                                      }
                                      onKeyDown={handleListKeyDown}
                                    >
                                      {options.map((option, i) => (
                                        <MenuItem
                                          key={option + group.id}
                                          onClick={e =>
                                            handleOptionGroup(e, group)
                                          }
                                        >
                                          {option}
                                        </MenuItem>
                                      ))}
                                    </MenuList>
                                  </ClickAwayListener>
                                </Paper>
                              </Grow>
                            )}
                          </Popper>
                        </NavLink>
                        <div className='LeftSideBar__more'>
                          <IconButton
                            id={`more${group.id}`}
                            ref={anchorRef}
                            aria-controls={
                              openId ? `more${group.id}` : undefined
                            }
                            aria-haspopup='true'
                            onClick={e => handleToggle(e, groupI)}
                          >
                            <MoreVertOutlinedIcon />
                          </IconButton>
                        </div>
                      </div>
                    )
                )}
              </InfiniteScroll>
            )}
          </List>
          <Divider />
          <List>
            {bottomLinks.map(bottomLink => (
              <NavLink
                key={bottomLink.text}
                className='LeftSideBar__bottom-item'
                activeClassName='LeftSideBar__bottom-item--active'
                to={bottomLink.path}
                onClick={handleClickBottomNav}
              >
                <ListItem className='LeftSideBar__bottomlistItem'>
                  <ListItemIcon>{bottomLink.icon}</ListItemIcon>
                  <ListItemText primary={bottomLink.text} />
                </ListItem>
              </NavLink>
            ))}
          </List>
        </div>
      </div>
    </nav>
  )
}

export default LeftSidebar
