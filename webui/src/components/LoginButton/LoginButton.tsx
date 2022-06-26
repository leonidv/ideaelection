import { useEffect } from 'react'
import { useRecoilValue } from 'recoil'

import { meInfoState } from './../../state'

import { Me } from '../../types/Me'
import { LoginButtonProps } from './LoginButtonInterfaces'

import { useTranslation } from 'react-i18next'

import logoGoogle from '../../images/google.svg'
import logoFacebook from '../../images/fb.svg'

import './LoginButton.scss'

const BACKEND_API_URL = process.env.BACKEND_API_URL;

export const LoginButton = (props: LoginButtonProps) => {
  // try {
    const me: Me = useRecoilValue(meInfoState) || null
    const { param } = props
    const { t } = useTranslation()

    let logoImg = logoGoogle
    let linkSrc =  `${BACKEND_API_URL}/oauth2/authorization/google`

    switch (param) {
      case 'Facebook': {
        logoImg = logoFacebook
        linkSrc = '/'
      }
    }

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
      window.location.href = linkSrc
    }

    return (
      <button onClick={handleClick} className={`loginButton loginButton--${param}`}>
        <img src={logoImg} className="loginButton__img" />
        <p className='loginButton__text'>{t('Continue with')} {param}</p>
      </button>
    )
  // } catch {
  //   const { t } = useTranslation()

  //   const handleClick = () => {
  //     window.location.href =
  //       `${BACKEND_API_URL}/oauth2/authorization/google`
  //   }

  //   return (
  //     <button onClick={handleClick} className='loginButton'>
  //       <div
  //         className='loginButton__img'
  //       ></div>
  //       {t('Login with Google')}
  //     </button>
  //   )
  // }
}
