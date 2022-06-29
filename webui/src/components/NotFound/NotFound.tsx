import { useTranslation } from "react-i18next"

import './NotFound.scss'


const NotFound: React.FC = () => {
    const { t } = useTranslation()
  
    return (
      <div className='notFound'>
        <h1 className='notFound__title'>SAEDI</h1>
        <h2 className='notFound__descr'>{t('Sorry, but page not found')}</h2>

        <a href="/" className="notFound__link">{t('Go to main page')}</a>
  
        <div className='row notFound__row'>
          <p className='notFound__descr'>{t('Your votes')}</p>
          <p className='notFound__descr'>{t('Your ideas')}</p>
        </div>
        <p className='notFound__copyright'>(C) saedi.io 2021</p>
      </div>
    )
}
  
export default NotFound