import { ChangeEvent, useEffect, useState } from 'react'
import { useRecoilState, useRecoilValue } from 'recoil'
import {
  Avatar,
  Button,
  Checkbox,
  FormControlLabel,
  Select,
  TextField
} from '@material-ui/core'
import { ModalButtons } from '../Modal/ModalButtons/ModalButtons'
import { meInfoState, tokenState } from '../../state'

import { useTranslation } from 'react-i18next'
import {
  fetchAccoutSettings,
  putSettings,
  fetchMe,
  updateToken
} from '../../functionsRequests'
import { ShowAlert } from '../Alert/Alert'

import {
  AccountSettingsProps,
  PlanSettings,
  Settings
} from './AccountSettingsInterfaces'
import { Me } from '../../types/Me'

import './AccountSettings.scss'

const notificationsOptions = ['Instantly', 'Daily', 'Weekly']

export const AccountSettings = (props: AccountSettingsProps): JSX.Element => {
  const { pictureUrl, setOpenModal } = props
  const me = useRecoilValue<Me>(meInfoState)
  const [token, setToken] = useRecoilState(tokenState)

  const { t } = useTranslation()
  const [planSettings, setPlanSettings] = useState<PlanSettings>({
    email: '',
    name: '',
    subscriptionPlan: me.subscriptionPlan
  })
  const [settings, setSettings] = useState<Settings>({
    checked: false,
    notifications: notificationsOptions[1]
  })
  const [isChanged, setIsChanged] = useState(false)

  const [severity, setSeverity]: any = useState('success')
  const [alertMessage, setAlertMessage] = useState('')
  const [openSnackbar, setOpenSnackbar] = useState(false)

  const showAlert = (open, severity, message) => {
    setOpenSnackbar(open)
    setSeverity(severity)
    setAlertMessage(message)
  }

  useEffect(() => {
    if (me.sub) {
      ;(async () => {
        fetchMe(token, me.sub).then(res => {
          if (res !== undefined && res !== 'undefined') {
            setPlanSettings(prevData => {
              return {
                ...prevData,
                email: res.email,
                name: res.displayName
              }
            })
          }
        })

        const currentSettings = await fetchAccoutSettings(token)
        if (await currentSettings) {
          setSettings(() => {
            return {
              checked: currentSettings.settings
                ? currentSettings.settings.subscribedToNews
                : settings.checked,
              notifications:
                currentSettings.settings &&
                currentSettings.settings.notificationsFrequency
                  ? currentSettings.settings.notificationsFrequency[0] +
                    currentSettings.settings.notificationsFrequency
                      .slice(1)
                      .toLowerCase()
                  : settings.notifications[1]
            }
          })
        } else {
          setPlanSettings(prevData => {
            return {
              ...prevData,
              email: me.email,
              name: me.displayName
            }
          })
        }
      })()
    }
  }, [me.sub])

  const handlePlanSettingsChange = (
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target

    setPlanSettings(prevData => {
      return {
        ...prevData,
        [name]: value
      }
    })
  }

  const handleSettingsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, checked } = e.target

    setSettings(prevData => {
      return {
        ...prevData,
        [name]: value || checked
      }
    })
  }

  const handleChangePlan = () => {
    setPlanSettings(prevData => {
      return {
        ...prevData,
        subscriptionPlan: 'ENTERPRISE'
      }
    })

    putSettings(token, settings, planSettings, 'ENTERPRISE').then(res => {
      if (res !== 'undefined' && res !== undefined) {
        showAlert(true, 'success', t('Tariff plan successfully updated'))
        if (res.mustReissueJwt) {
          updateToken(token).then(newToken => {
            if (newToken !== 'undefined' && newToken !== undefined) {
              localStorage.setItem('jwt', newToken)
              setToken(newToken)
            }
          })
        }
      } else {
        showAlert(true, 'error', t('Something went wrong'))
      }
    })

    setTimeout(() => {
      setOpenSnackbar(false)
    }, 2000)

    setIsChanged(true)
  }

  const handleAccept = () => {
    putSettings(token, settings, planSettings).then(res => {
      if (res !== 'undefined' && res !== undefined) {
        showAlert(true, 'success', t('Account settings successfully updated'))
        if (res.mustReissueJwt) {
          updateToken(token).then(newToken => {
            if (newToken !== 'undefined' && newToken !== undefined) {
              localStorage.setItem('jwt', newToken)
              setToken(newToken)
            }
          })
        }
      } else {
        showAlert(true, 'error', t('Something went wrong'))
      }
      setTimeout(() => {
        setOpenSnackbar(false)
        setOpenModal(false)
      }, 2000)
    })
  }

  const handleClose = () => {
    setOpenModal(false)
  }

  return (
    <div className='accountSettings'>
      <h1 className='accountSettings__title'>{t('Account Settings')}</h1>
      <h2 className='accountSettings__subtitle'>{t('Profile')}</h2>
      <div className='row accountSettings__row'>
        <Avatar alt='avatar accountSettings__avatar' src={pictureUrl} />
        <div className='col'>
          <TextField
            label={t('Name')}
            name='name'
            className='accountSettings__textField'
            value={planSettings.name}
            onChange={handlePlanSettingsChange}
          />
          <TextField
            label={t('Email')}
            name='email'
            value={planSettings.email}
            disabled
          />
        </div>
      </div>

      <h2 className='accountSettings__subtitle'>{t('Subscription plan')}</h2>
      <div className='row accountSettings__row accountSettings__row--center'>
        <div className='accountSettings__plan'>
          {!isChanged && planSettings.subscriptionPlan == 'FREE' && (
            <>
              <p className='accountSettings__plan-text'>Free</p>
              <p className='accountSettings__plan-text'>
                {t('10 members in private groups')}
              </p>
              <p className='accountSettings__plan-text'>
                {t("You can't set domain restriction")}
              </p>
            </>
          )}
          {(isChanged || planSettings.subscriptionPlan !== 'FREE') && (
            <p className='accountSettings__plan-text'>
              {t('Thanks for readiness to pay!')}
            </p>
          )}
        </div>
        {!isChanged && planSettings.subscriptionPlan == 'FREE' && (
          <Button
            className='accountSettings__btn'
            onClick={handleChangePlan}
            variant='contained'
            color='primary'
          >
            {t('CHANGE')}
          </Button>
        )}
      </div>

      <h2 className='accountSettings__subtitle'>{t('Notifications')}</h2>
      <div className='row accountSettings__row accountSettings__row--center'>
        <Select
          className='accountSettings__select'
          defaultValue={notificationsOptions[1]}
          value={settings.notifications}
          inputProps={{
            name: 'notifications'
          }}
          onChange={handleSettingsChange}
        >
          {notificationsOptions.map(option => (
            <option
              key={option}
              className='accountSettings__option'
              value={option}
            >
              {t(option)}
            </option>
          ))}
        </Select>
        <div className='accountSettings__notifications'>
          <p className='accountSettings__notifications-text'>
            {t('Notifications about new ideas')}
          </p>
        </div>
      </div>

      <h2 className='accountSettings__subtitle accountSettings__subtitle--noMargin'>
        {t('Platform news')}
      </h2>
      <FormControlLabel
        control={
          <Checkbox
            checked={settings.checked}
            onChange={handleSettingsChange}
            name='checked'
            color='primary'
          />
        }
        label={t('I want to receive news about platform updates')}
      />
      <ModalButtons
        acceptText={t('UPDATE')}
        handleAccept={handleAccept}
        handleClose={handleClose}
      />
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
