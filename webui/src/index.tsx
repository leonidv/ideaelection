import React, { Suspense } from 'react'
import ReactDOM from 'react-dom'
import App from './App'
import { RecoilRoot } from 'recoil'
import { useTranslation } from 'react-i18next'
import './index.scss'
import './i18n'
import { CircularProgress } from '@material-ui/core'
import { ErrorBoundary } from 'react-error-boundary'
import { ShowAlert } from './components/Alert/Alert'
import { MainScreen } from './components/MainScreen/MainScreen'

import '@fontsource/roboto'

function ErrorFallback ({ error, resetErrorBoundary }) {
  const { t } = useTranslation()

  return (
    <div className='app'>
      <MainScreen />
      <ShowAlert
        open={true}
        severity='error'
        message={t('Something went wrong! Please try login again!')}
      />
    </div>
  )
}

ReactDOM.render(
  <React.StrictMode>
    <RecoilRoot>
      <Suspense
        fallback={
          <div className='app__suspense'>
            <CircularProgress />
          </div>
        }
      >
        <ErrorBoundary FallbackComponent={ErrorFallback} onReset={() => {}}>
          <App />
        </ErrorBoundary>
      </Suspense>
    </RecoilRoot>
  </React.StrictMode>,
  document.getElementById('root')
)
