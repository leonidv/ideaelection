import { useTranslation } from "react-i18next"


export const NotFound: React.FC = () => {
    const { t } = useTranslation()
  
    return (
      <div className='mainScreen'>
        <h1 className='mainScreen__title'>SAEDI</h1>
        <h2 className='mainScreen__descr'>{t('Sorry, but page not found')}</h2>

        <a href="/" className="mainScreen__link">{t('Go to main page')}</a>
  
        <div className='row mainScreen__row'>
          <p className='mainScreen__descr'>{t('Your votes')}</p>
          <p className='mainScreen__descr'>{t('Your ideas')}</p>
        </div>
        <p className='mainScreen__copyright'>(C) saedi.io 2021</p>
      </div>
    )
  }