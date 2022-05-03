import { Members } from "../../../../types/Members";

export interface GroupMainScreenInviteProps {
    invite: any
    invites: any
    setInvites: (any) => void
    param?: string
    isParamFound?: boolean
    localParam?: string
    isModal?: boolean
    fetchMoreDataInvites?: (a?:boolean, b?:any) => void
    fetchMoreData?: (a:boolean, b:any, c:string) => void
    setMembers?: any
    handleCloseJoinRequestModal?: () => void
    showAlert: (open: any, severity: any, message: any) => void; 
}