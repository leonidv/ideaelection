import { Button } from '@material-ui/core'

import { ModalButtonsProps } from './ModalButtonsInterfaces'

import { useTranslation } from 'react-i18next'

import './ModalButtons.scss'

export const ModalButtons: React.FC<ModalButtonsProps> = (
  props: ModalButtonsProps
) => {
  const { acceptText, handleClose, handleAccept } = props

  const { t } = useTranslation()

  return (
    <div className='modalButtons'>
      <Button className='modalButtons__cancel' onClick={handleClose}>
        {t('CANCEL')}
      </Button>
      <Button
        className='modalButtons__accept'
        onClick={handleAccept}
        variant='contained'
        color='primary'
      >
        {t(acceptText)}
      </Button>
    </div>
  )
}
