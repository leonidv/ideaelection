export interface AccountSettingsProps {
  pictureUrl: string
  setOpenModal: (any) => void
}

export interface PlanSettings {
  email: string
  name: string,
  subscriptionPlan: string
}

export interface Settings {
  checked: boolean
  notifications: string
}
