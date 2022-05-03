import { useState } from 'react'
import { useRecoilState } from 'recoil'
import { groupByKeyState } from './../../../state'

import { GroupMainScreenInvite } from '../../Group/GroupMainScreen/GroupMainScreenInvite/GroupMainScreenInvite'
import { Modal } from '../../Modal/Modal'

import './JoinRequestsModal.scss'
import { useHistory } from 'react-router'
import { JoinRequestsModalProps } from './JoinRequestsModalInterfaces'

export const JoinRequestsModal = (props: JoinRequestsModalProps) => {
  const { showAlert } = props
  const [request, setRequest]: any = useRecoilState(groupByKeyState)
  const [open, setOpen] = useState(true)
  const history = useHistory()

  const handleClose = () => {
    setTimeout(() => {
      history.push('/main')
    }, 1000)
    localStorage.removeItem('inviteKey')
  }

  return (
    open && (
      <Modal className='joinRequestsModal' open={open} onClose={handleClose}>
        {request && Array.isArray(request) && request[0] && (
          <GroupMainScreenInvite
            invite={request[0]}
            invites={request}
            setInvites={setRequest}
            param='available'
            isModal={open}
            handleCloseJoinRequestModal={handleClose}
            showAlert={showAlert}
          />
        )}
      </Modal>
    )
  )
}
