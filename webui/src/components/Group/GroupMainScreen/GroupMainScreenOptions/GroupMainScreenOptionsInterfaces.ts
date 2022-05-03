import { Idea } from "../../../../types/Idea";

export interface GroupMainScreenOptionsProps {
    options: string[]
    id: string 
    handleOption: (e: any, idea: Idea) => void 
    idea?: Idea
    anchorRef: React.MutableRefObject<any>
}