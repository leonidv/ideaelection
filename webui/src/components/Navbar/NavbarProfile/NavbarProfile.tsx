import { MouseEvent, useRef, useState } from 'react'
import { Avatar, Button, Menu, MenuItem } from '@material-ui/core'
import { AccountSettings } from '../../AccountSettings/AccountSettings'
import { Modal } from '../../Modal/Modal'

import { NavbarProfileProps } from './NavbarProfileInterfaces'

import './NavbarProfile.scss'
import { useTranslation } from 'react-i18next'

const options = ['Account', 'Logout']

export default function NavbarProfile (props: NavbarProfileProps): JSX.Element {
  const pictureUrl = props.pictureUrl

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [openModal, setOpenModal] = useState<boolean>(false)

  const refModal = useRef(Modal)
  const {t} = useTranslation()

  const handleOpenModal = () => {
    setOpenModal(true)
  }

  const handleCloseModal = () => {
    setOpenModal(false)
  }

  const handleClick = (event: MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleClose = () => {
    setAnchorEl(null)
  }

  const handleOption = option => {
    switch (option) {
      case 'Account':
        handleOpenModal()
        break
      case 'Logout':
        localStorage.removeItem('jwt')
        window.location.href = '/'
        break
    }

    handleClose()
  }

  return (
    <div className='navbarProfile__profile'>
      <Button className='navbarProfile__avatar-btn' onClick={handleClick}>
        <Avatar alt='avatar' src={pictureUrl} />
      </Button>
      {Boolean(anchorEl) && (
        <Menu
          id='navbarProfile__menu'
          anchorEl={anchorEl}
          keepMounted
          open={Boolean(anchorEl)}
          onClose={handleClose}
        >
          {options.map(option => (
            <MenuItem key={option} onClick={() => handleOption(option)}>
              {t(option)}
            </MenuItem>
          ))}
        </Menu>
      )}
      {openModal && (
        <Modal open={openModal} onClose={handleCloseModal} ref={refModal}>
          <AccountSettings
            pictureUrl={pictureUrl}
            setOpenModal={setOpenModal}
          />
        </Modal>
      )}
    </div>
  )
}
