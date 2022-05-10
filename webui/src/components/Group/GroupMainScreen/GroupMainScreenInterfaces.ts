export interface GroupMainScreenProps {
  requests?: any
  setRequests?: (any) => void
  param: any
  states?: any
  switchParams?: any
  switchParamsOrdering: any
  handleChangeSearch?: (any) => void,
  fetchMoreData?: (a, b) => void
  switchAlert?: () => void
  showAlert: (open: any, severity: any, message: any) => void; 
}
