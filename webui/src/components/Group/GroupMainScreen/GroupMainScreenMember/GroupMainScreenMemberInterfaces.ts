import { Groups } from "../../../../types/Groups";
import { Members } from "../../../../types/Members";

export interface GroupMainScreenMemberProps {
    member: any
    param: string
    changedMember: Groups | Members
    setChangedMember: (any) => void
    paramMain?: string
    setMembers?: (any) => void
    showAlert: (boolean, Color, string) => void
    isAdmin?: boolean
    members?: Members[]
    setLocalMembers?: any
}