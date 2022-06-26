import { LoginButton } from '../LoginButton/LoginButton'
import { useTranslation, getI18n } from 'react-i18next'

import logo from '../../images/logo.svg'
import logoMobile from '../../images/logo-mobile.svg'
import { quotes } from './quotes'

const quote = quotes[Math.floor(Math.random() * quotes.length)]

import './MainScreen.scss'

const MainScreen: React.FC = () => {
  const { t } = useTranslation()
  const lang = getI18n().language

  return (
    <div className='mainScreen row'>
      <div className='col mainScreen__col mainScreen__col--left'>
        <img src={logo} className='mainScreen__logo' />

        <div className='mainScreen__questions'>
          <p className='mainScreen__question'>{t(quote)}</p>
        </div>
      </div>

      <img
        src={logoMobile}
        className='mainScreen__logo mainScreen__logo--mobile'
      />

      <div className='col mainScreen__col mainScreen__col--right'>
        <h1 className='mainScreen__title'>{t('Welcome to')} Saedi</h1>
        <h2 className='mainScreen__descr'>{t('The best place')}</h2>
        <LoginButton param='Google' />
        <LoginButton param='Facebook' />
        <p className='mainScreen__privacy'>
          {t('This site is protected')}
          {lang == 'ru' ? `, ${t('apply')} ` : ' '}
          <a href='#' target='_blank'>
            {t('Privacy Policy')}
          </a>
          {' ' + t('and') + ' '}
          <a href='#' target='_blank'>
            {t('Terms of Service')}
          </a>
          {lang !== 'ru' ? ` ${t('apply')}` : ''}.
        </p>
      </div>
    </div>
  )
}

export default MainScreen
