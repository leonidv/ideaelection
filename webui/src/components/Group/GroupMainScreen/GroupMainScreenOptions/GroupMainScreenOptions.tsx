import { useState } from 'react'
import {
  IconButton,
  ClickAwayListener,
  Grow,
  Paper,
  Popper,
  MenuList,
  MenuItem
} from '@material-ui/core'
import MoreVertIcon from '@material-ui/icons/MoreVert'

import './GroupMainScreenOptions.scss'
import { GroupMainScreenOptionsProps } from './GroupMainScreenOptionsInterfaces'
import { useTranslation } from 'react-i18next'

export const GroupMainScreenOptions: React.FC<GroupMainScreenOptionsProps> = (
  props: GroupMainScreenOptionsProps
) => {
  const { options, id, handleOption, idea, anchorRef, keepMounted } = props
  if (options.length > 0) {
    const [openId, setOpenId]: any = useState(false)

    const handleToggle = e => {
      let id = null
      let el = e.target

      while (!id) {
        id = el.id
        el = el.parentElement
      }

      !!openId == false ? setOpenId(id) : setOpenId(false)
    }

    const handleClose = event => {
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

    const {t} = useTranslation()

    return (
      <>
        <IconButton
          aria-label='options'
          id={id}
          ref={anchorRef}
          aria-controls={openId ? id : undefined}
          aria-haspopup='true'
          onClick={handleToggle}
        >
          <MoreVertIcon />
        </IconButton>
        <Popper
          className='groupMainScreenOptions__openMenu'
          open={openId == id ? true : false}
          anchorEl={anchorRef.current}
          role={undefined}
          transition
          keepMounted={keepMounted || false}
        >
          {({ TransitionProps, placement }) => (
            <Grow
              {...TransitionProps}
              style={{
                transformOrigin:
                  placement === 'bottom' ? 'center top' : 'center bottom'
              }}
            >
              <Paper>
                <ClickAwayListener onClickAway={handleClose}>
                  <MenuList
                    autoFocusItem={openId == id ? true : false}
                    onKeyDown={handleListKeyDown}
                  >
                    {options.map(option => (
                      <MenuItem
                        key={option + id}
                        onClick={e => {
                          handleClose(e)
                          handleOption(e, idea)
                        }}
                      >
                        {t(option)}
                      </MenuItem>
                    ))}
                  </MenuList>
                </ClickAwayListener>
              </Paper>
            </Grow>
          )}
        </Popper>
      </>
    )
  } else {
    return <></>
  }
}
