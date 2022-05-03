import { useState } from 'react'
import { Modal as ModalMaterial } from '@material-ui/core'

import { modalProps } from './ModalInterfaces'

import './Modal.scss'

function getModalStyle () {
  return {
    left: '50%',
    top: '50%',
    transform: 'translate(-50%, -50%)'
  }
}

export const Modal: React.FC<modalProps> = (props: modalProps) => {
  const { open, onClose, title, className } = props
  const [modalStyle] = useState(getModalStyle)

  return (
    <ModalMaterial open={open} onClose={onClose} className={className}>
      <div style={modalStyle} className='modal'>
        {title && <h1 className='modal__title'>{title}</h1>}
        {props.children}
      </div>
    </ModalMaterial>
  )
}
