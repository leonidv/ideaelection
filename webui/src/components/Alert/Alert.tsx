import { useState } from 'react'
import { Snackbar } from '@material-ui/core'
import MuiAlert, { AlertProps } from '@material-ui/lab/Alert'

import { ShowAlertProps } from './AlertInterfaces'
import { useTranslation } from 'react-i18next'

export const ShowAlert = (props: ShowAlertProps) => {
  const { open, severity, message } = props
  const [openSnackbar, setOpenSnackbar] = useState(open)

  const {t} = useTranslation()

  const handleCloseSnackbar = () => {
    setTimeout(() => setOpenSnackbar(false), 1000)
  }

  return (
    <Snackbar
      open={openSnackbar}
      autoHideDuration={3000}
      onClose={handleCloseSnackbar}
    >
      <Alert onClose={handleCloseSnackbar} severity={severity}>
        {t(message)}
      </Alert>
    </Snackbar>
  )
}

export const Alert = (props: AlertProps) => {
  return <MuiAlert elevation={6} variant='filled' {...props} />
}
