import { LoginButton } from '../LoginButton/LoginButton'
import { useTranslation } from 'react-i18next'

import './MainScreen.scss'

const MainScreen: React.FC = () => {
  const { t } = useTranslation()

  return (
    <div className='mainScreen'>
      <h1 className='mainScreen__title'>SAEDI</h1>
      <LoginButton />

      <div className='row mainScreen__row'>
        <p className='mainScreen__descr'>{t('Your votes')}</p>
        <p className='mainScreen__descr'>{t('Your ideas')}</p>
      </div>
      <p className='mainScreen__copyright'>(C) saedi.io 2022</p>
    </div>
  )
}

export default MainScreen;
