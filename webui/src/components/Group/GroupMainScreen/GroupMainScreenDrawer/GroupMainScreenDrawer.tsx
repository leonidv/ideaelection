import { useEffect, useState } from 'react'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Button,
  DialogTitle,
  Divider,
  List,
  ListItem,
  ListItemText,
  Switch
} from '@material-ui/core'
import {
  ExpandMore as ExpandMoreIcon,
  Settings as SettingsIcon
} from '@material-ui/icons'

import { SearchShortList } from '../../SearchShortList/SearchShortList'
import { GroupMainScreenSettings } from '../GroupMainScreenSettings/GroupMainScreenSettings'

import { GroupMainScreenDrawerProps } from './GroupMainScreenDrawerInterfaces'

import './groupMainScreenDrawer.scss'

import { useTranslation } from 'react-i18next'

export const GroupMainScreenDrawer = (props: GroupMainScreenDrawerProps) => {
  const {
    param,
    switchParams,
    state,
    handleChangeSwitch,
    handleManageJoinRequests,
    members,
    setMembers,
    handleInviteMembers,
    handleClickSettings,
    group,
    openGroupMainScreenSettings,
    setOpenGroupMainScreenSettings,
    handleClose,
    me
  } = props

  const { t } = useTranslation()

  const [isAdmin, setIsAdmin] = useState(false)

  useEffect(() => {}, [state])

  useEffect(() => {}, [members])

  useEffect(() => {
    if (param == 'ideas' && group && group.creator.id && me && me.sub) {
      if (group.creator.id == me.sub) {
        setIsAdmin(true)
      } else {
        setIsAdmin(false)
      }
    }
  }, [group])

  // const membersPlan = group && group.entryMode && group.entryMode == 'PRIVATE' ? maxMembers : '\u221E';

  const membersPlan = '\u221E'

  return (
    <>
      <div>
        <p className='groupMainScreenDrawer__description'>
          {param == 'ideas' && group.description}
          {param == 'group' &&
            t('Join public groups and share ideas with their members')}
          {(param == 'invites' || param == 'requests') &&
            t('Accept the invitation form')}
        </p>
        {switchParams && (
          <Accordion className='groupMainScreenDrawer__accordeon'>
            <AccordionSummary
              className='groupMainScreenDrawer__filter'
              expandIcon={
                <ExpandMoreIcon className='groupMainScreenDrawer__expandIcon' />
              }
              aria-controls='panel1a-content'
              id='panel1a-header'
            >
              {param == 'ideas' && t('Filter ideas')}
              {param == 'group' && t('Filter groups')}
            </AccordionSummary>
            <AccordionDetails>
              <List className='groupMainScreenDrawer__filterList'>
                {switchParams.map(switchParam => (
                  <ListItem
                    key={switchParam.title}
                    className='groupMainScreenDrawer__filterListItem'
                  >
                    <ListItemText
                      className='groupMainScreenDrawer__filterText'
                      primary={switchParam.title}
                    />
                    <Switch
                      checked={state[switchParam.name]}
                      onChange={handleChangeSwitch}
                      color='primary'
                      name={switchParam.name}
                      inputProps={{ 'aria-label': 'primary checkbox' }}
                    />
                  </ListItem>
                ))}
              </List>
            </AccordionDetails>
          </Accordion>
        )}

        {param == 'ideas' && (
          <div>
            <div className='groupMainScreenDrawer__top-member row'>
              <DialogTitle className='groupMainScreenDrawer__title-member'>
                {t('Members')}
              </DialogTitle>

              <div className='groupMainScreenDrawer__top-right'>
                <span className='groupMainScreenDrawer__members'>
                  {members && members.length}/{membersPlan}
                </span>
              </div>
            </div>
            <SearchShortList
              param='groupMain'
              isAdmin={isAdmin}
              curMembers={members}
              setMembers={setMembers}
            />
          </div>
        )}
      </div>
      {param == 'ideas' && (
        <div>
          <Divider />
          {isAdmin && (
            <Button
              variant='contained'
              className='groupMainScreenDrawer__settings-btn'
              onClick={handleInviteMembers}
            >
              {t('INVITE USERS TO GROUP')}
            </Button>
          )}
          {isAdmin && (
            <Button
              id='manageJoinRequest'
              variant='contained'
              className='groupMainScreenDrawer__settings-btn'
              onClick={handleManageJoinRequests}
            >
              {t('MANAGE JOIN REQUESTS')}
            </Button>
          )}

          <Divider />
          <Button
            variant='contained'
            className='groupMainScreenDrawer__settings-btn'
            startIcon={<SettingsIcon />}
            onClick={handleClickSettings}
          >
            {t('GROUP PROPERTIES')}
          </Button>
          {Object.keys(group).length && openGroupMainScreenSettings && (
            <GroupMainScreenSettings
              openGroupMainScreenSettings={openGroupMainScreenSettings}
              setOpenGroupMainScreenSettings={setOpenGroupMainScreenSettings}
              handleClose={handleClose}
            />
          )}
        </div>
      )}
    </>
  )
}
