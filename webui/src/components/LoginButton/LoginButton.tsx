import { useEffect } from 'react'
import { useRecoilValue } from 'recoil'

import { meInfoState } from './../../state'

import { Me } from '../../types/Me'

import { useTranslation } from 'react-i18next'

import './LoginButton.scss'

export const LoginButton: React.FC = () => {
  try {
    const me: Me = useRecoilValue(meInfoState) || null
    const { t } = useTranslation()

    useEffect(() => {
      const key = localStorage.getItem('inviteKey')
      if (!key && window.location.href.includes('inviteLink=')) {
        localStorage.setItem(
          'inviteKey',
          window.location.href.split('inviteLink=')[1]
        )
      }
      if (key && me && me.sub) {
        window.location.href = `/groups/inviteLink=${key}`
      } else if (me && me.sub) {
        window.location.href = '/main'
      }
    }, [me])

    const handleClick = () => {
      window.location.href =
        'https://api.test.saedi.io/oauth2/authorization/google'
    }

    return (
      <button onClick={handleClick} className='loginButton'>
        <img
          alt='login'
          className='loginButton__img'
          src='../../images/google.svg'
        />
        {t('Login with Google')}
      </button>
    )
  } catch {
    const { t } = useTranslation()

    const handleClick = () => {
      window.location.href =
        'https://api.test.saedi.io/oauth2/authorization/google'
    }

    return (
      <button onClick={handleClick} className='loginButton'>
        <img
          alt='login'
          className='loginButton__img'
          src='../../images/google.svg'
        />
        {t('Login with Google')}
      </button>
    )
  }
}
