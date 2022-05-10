import { useLayoutEffect, useState } from 'react'
import { AppBar, Container, Toolbar, IconButton } from '@material-ui/core'
import { Menu as MenuIcon, Close as CloseIcon } from '@material-ui/icons'
import InsertLinkIcon from '@material-ui/icons/InsertLink'
import { CopyToClipboard } from 'react-copy-to-clipboard'
import { useRecoilState, useRecoilValue } from 'recoil'

import {
  currentGroupState,
  meInfoState,
  mobileOpenRightState,
} from '../../state'

import { defaultGroups } from '../../types/Groups'

import NavbarProfile from './NavbarProfile/NavbarProfile'

import './Navbar.scss'
import { useTranslation } from 'react-i18next'


export default function Navbar (props): JSX.Element {
  const [group, setGroup] = useRecoilState(currentGroupState)
  const [localGroup, setLocalGroup]: any = useState([])
  const windowLink = window.location.href
  const { handleDrawerToggle, handleDrawerToggleRight, showAlert } = props
  const me = useRecoilValue(meInfoState)
  const isMobileOpenRight = useRecoilValue(mobileOpenRightState)

  const {t} = useTranslation()

  useLayoutEffect(() => {
    if (windowLink.split('/')[3] == 'group') {
      if (Object.keys(group) && group.id) {
        setLocalGroup(group)
        document.title = group.name
      } 
    } else {
      setLocalGroup([])
      setGroup(defaultGroups)
      document.title = 'Saedi'
    }
  }, [group, windowLink])


  return (
    <nav>
      <AppBar className='navbar__appBar' position='static'>
        <Container className='navbar__container'>
          <IconButton
            color='inherit'
            className='navbar__icon-left'
            aria-label='open drawer'
            edge='start'
            onClick={handleDrawerToggle}
          >
            <MenuIcon />
          </IconButton>
          <Toolbar className='navbar__toolbar'>
            {Object.keys(localGroup) &&
              localGroup.name &&
              window.location.href.indexOf('/group/') != -1 && (
                <>
                  <h2 className='navbar__group-name'>{localGroup.name}</h2>
                  <CopyToClipboard text={windowLink}>
                    <IconButton className='navbar__button' onClick={() => {
                    showAlert(true, 'success', t('Link copied successfully'))
                  }}>
                      <InsertLinkIcon />
                    </IconButton>
                  </CopyToClipboard>
                </>
              )}
            <NavbarProfile pictureUrl={me.avatar} />
          </Toolbar>
          {window.location.pathname !== '/main' && (
            <IconButton
              color='inherit'
              className='navbar__icon-right'
              aria-label='open drawer'
              edge='start'
              onClick={handleDrawerToggleRight}
            >
              {isMobileOpenRight && <CloseIcon />}
              {!isMobileOpenRight && <MenuIcon />}
            </IconButton>
          )}
        </Container>
      </AppBar>
    </nav>
  )
}
